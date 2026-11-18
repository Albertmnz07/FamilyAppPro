import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef, useState } from "react";
import { useParams } from "react-router";
import { getFamily } from "../api/families";
import { issueInvite, type IssuedInvite } from "../api/invites";
import {
  getDniPhoto,
  getMemberDni,
  listFamilyMembers,
  removeMember,
  revokeConsent,
  type Member,
} from "../api/members";
import { messageForDniFailure, messageForFailure } from "../api/messages";
import { getPersonaId } from "../auth/token";

/** The backend sends an ISO instant; the browser shows it in local time (spec #14, "Texts"). */
function formatInviteExpiry(expiresAt: string): string {
  return new Date(expiresAt).toLocaleString("es-ES", { dateStyle: "long", timeStyle: "short" });
}

/** Syncs fetched photo bytes to the DOM and releases the object URL on change. */
function PhotoImage({ blob, alt }: { blob: Blob; alt: string }) {
  const ref = useRef<HTMLImageElement | null>(null);
  useEffect(() => {
    const element = ref.current;
    if (!element) return;
    const url = URL.createObjectURL(blob);
    element.src = url;
    return () => {
      URL.revokeObjectURL(url);
      element.removeAttribute("src");
    };
  }, [blob]);
  return <img ref={ref} alt={alt} />;
}

/** The destructive action a confirmation dialog is guarding, and its target. */
type PendingAction =
  | { kind: "revoke"; member: Member }
  | { kind: "remove"; member: Member };

/** The copy each confirmation dialog shows; one branch on the action's kind. */
function confirmationFor(action: PendingAction) {
  if (action.kind === "revoke") {
    return {
      heading: "¿Revocar tu consentimiento?",
      body: "Tu DNI dejará de ser visible en esta familia. Solo una nueva invitación puede restaurarlo.",
      confirmLabel: "Confirmar revocación",
    };
  }
  return {
    heading: `¿Retirar a ${action.member.username}?`,
    body: "La persona dejará de aparecer en la lista de miembros de esta familia.",
    confirmLabel: "Confirmar retirada",
  };
}

export function FamilyWorkspacePage() {
  const { familyId = "" } = useParams<{ familyId: string }>();
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<Member | null>(null);
  const [invite, setInvite] = useState<IssuedInvite | null>(null);
  const [copyState, setCopyState] = useState<"idle" | "copied" | "failed">("idle");
  const [pendingAction, setPendingAction] = useState<PendingAction | null>(null);
  const currentPersonaId = getPersonaId();
  const queryClient = useQueryClient();
  const dniHeadingRef = useRef<HTMLHeadingElement | null>(null);
  const confirmRef = useRef<HTMLDialogElement | null>(null);
  // The Family carries the Admin's Persona; only that Persona may issue Invites.
  const familyQuery = useQuery({
    queryKey: ["family", familyId],
    queryFn: ({ signal }) => getFamily(familyId, signal),
  });
  const isAdmin =
    currentPersonaId !== null && familyQuery.data?.adminPersonaId === currentPersonaId;
  const inviteMutation = useMutation({
    mutationFn: () => issueInvite(familyId),
    onSuccess: (issued) => {
      setInvite(issued);
      setCopyState("idle");
    },
  });
  // Revoking a Consent and removing a Member are the two guarded destructive
  // actions; one mutation keeps a single error state and refreshes the list.
  const actionMutation = useMutation({
    mutationFn: (action: PendingAction) =>
      action.kind === "revoke"
        ? revokeConsent(familyId, action.member.personaId)
        : removeMember(familyId, action.member.personaId),
    onSuccess: (_data, action) => {
      setSelected((current) =>
        current?.personaId === action.member.personaId ? null : current,
      );
      void queryClient.invalidateQueries({ queryKey: ["members", familyId] });
    },
  });
  const actionError = actionMutation.error;
  const membersQuery = useQuery({
    queryKey: ["members", familyId, page],
    queryFn: ({ signal }) => listFamilyMembers(familyId, page, signal),
  });
  const dniQuery = useQuery({
    queryKey: ["dni", familyId, selected?.personaId ?? null],
    queryFn: ({ signal }) => {
      if (!selected) throw new Error("No member selected");
      return getMemberDni(familyId, selected.personaId, signal);
    },
    enabled: selected !== null,
  });
  const frontPhotoUrl = dniQuery.data?.frontPhotoUrl;
  const backPhotoUrl = dniQuery.data?.backPhotoUrl;
  const frontPhotoQuery = useQuery({
    queryKey: ["dni-photo", familyId, selected?.personaId, "front", frontPhotoUrl],
    queryFn: ({ signal }) => {
      if (!frontPhotoUrl) throw new Error("No front photo URL");
      return getDniPhoto(frontPhotoUrl, signal);
    },
    enabled: frontPhotoUrl !== undefined,
  });
  const backPhotoQuery = useQuery({
    queryKey: ["dni-photo", familyId, selected?.personaId, "back", backPhotoUrl],
    queryFn: ({ signal }) => {
      if (!backPhotoUrl) throw new Error("No back photo URL");
      return getDniPhoto(backPhotoUrl, signal);
    },
    enabled: backPhotoUrl !== undefined,
  });
  const dniFailure = dniQuery.error ?? frontPhotoQuery.error ?? backPhotoQuery.error;
  const pageData = membersQuery.data;
  const members = pageData?.content ?? [];
  const confirmation = pendingAction ? confirmationFor(pendingAction) : null;

  // A removal can shrink the list below the page the user is on; fall back to
  // the last page that still exists rather than showing an empty page. Adjusting
  // during render (React's supported pattern) avoids a wasted empty frame.
  if (pageData && page >= pageData.totalPages) {
    setPage(Math.max(0, pageData.totalPages - 1));
  }

  useEffect(() => {
    if (selected) dniHeadingRef.current?.focus();
  }, [selected]);

  // Open the native modal as soon as a destructive action is pending. The
  // browser then provides the top layer, focus containment and Esc dismissal.
  useEffect(() => {
    if (!pendingAction) return;
    const dialog = confirmRef.current;
    if (dialog && !dialog.open) dialog.showModal();
  }, [pendingAction]);

  function handleConfirmClose() {
    const dialog = confirmRef.current;
    const confirmed = dialog?.returnValue === "confirm";
    if (dialog) dialog.returnValue = "";
    const action = pendingAction;
    setPendingAction(null);
    if (!confirmed || !action) return;
    actionMutation.mutate(action);
  }

  function changePage(next: number) {
    setSelected(null);
    setPage(next);
  }

  async function copyInviteCode(code: string) {
    try {
      await navigator.clipboard.writeText(code);
      setCopyState("copied");
    } catch {
      setCopyState("failed");
    }
  }

  return (
    <section className="page">
      <h1>Familia</h1>
      {membersQuery.isPending && (
        <p className="loading" role="status">
          Cargando miembros…
        </p>
      )}
      {membersQuery.isError && (
        <p className="form-error" role="alert">
          {messageForFailure(membersQuery.error)}
          <button type="button" onClick={() => void membersQuery.refetch()}>
            Reintentar
          </button>
        </p>
      )}
      {actionError && (
        <p className="form-error" role="alert">
          {messageForFailure(actionError)}
        </p>
      )}
      {pageData && (
        <>
          {pageData.totalElements > 0 ? (
            <div className="member-table-wrapper">
              <table className="member-table">
                <caption>Miembros de la familia</caption>
                <thead>
                  <tr>
                    <th scope="col">Nombre de usuario</th>
                    <th scope="col">DNI</th>
                    <th scope="col">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {members.map((member) => (
                    <tr key={member.personaId}>
                      <th scope="row" data-label="Nombre de usuario">
                        {member.username}
                        {member.admin && <span className="badge">Admin</span>}
                      </th>
                      <td data-label="DNI">
                        {member.consentInForce && member.dni ? member.dni : "Sin consentimiento"}
                      </td>
                      <td data-label="Acciones">
                        <div className="row-actions">
                          {member.consentInForce && (
                            <button
                              type="button"
                              aria-label={`Ver DNI de ${member.username}`}
                              onClick={() => setSelected(member)}
                            >
                              Ver DNI
                            </button>
                          )}
                          {member.personaId === currentPersonaId && member.consentInForce && (
                            <button
                              type="button"
                              aria-label="Revocar consentimiento"
                              onClick={() => setPendingAction({ kind: "revoke", member })}
                            >
                              Revocar consentimiento
                            </button>
                          )}
                          {isAdmin && member.personaId !== currentPersonaId && (
                            <button
                              type="button"
                              className="danger"
                              aria-label={`Retirar a ${member.username}`}
                              onClick={() => setPendingAction({ kind: "remove", member })}
                            >
                              Retirar
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="empty-state">
              <p>Esta familia todavía no tiene miembros.</p>
            </div>
          )}
          {pageData.totalPages > 0 && (
            <nav className="pagination" aria-label="Paginación de miembros">
              <button
                type="button"
                disabled={pageData.number <= 0}
                onClick={() => changePage(pageData.number - 1)}
              >
                Anterior
              </button>
              <span>
                Página {pageData.number + 1} de {pageData.totalPages}
              </span>
              <button
                type="button"
                disabled={pageData.number >= pageData.totalPages - 1}
                onClick={() => changePage(pageData.number + 1)}
              >
                Siguiente
              </button>
            </nav>
          )}
        </>
      )}
      {isAdmin && (
        <section className="invite-issuance" aria-labelledby="invite-heading">
          <h2 id="invite-heading">Invitación</h2>
          {inviteMutation.isError && (
            <p className="form-error" role="alert">
              {messageForFailure(inviteMutation.error)}
            </p>
          )}
          <button
            type="button"
            disabled={inviteMutation.isPending}
            onClick={() => inviteMutation.mutate()}
          >
            {inviteMutation.isPending ? "Emitiendo…" : "Emitir invitación"}
          </button>
          {invite && (
            <div className="invite-details">
              <p>
                <span className="invite-label">Código:</span>{" "}
                <code className="invite-code">{invite.code}</code>
              </p>
              <p>
                <span className="invite-label">Caduca:</span>{" "}
                <time dateTime={invite.expiresAt}>{formatInviteExpiry(invite.expiresAt)}</time>
              </p>
              <button type="button" onClick={() => void copyInviteCode(invite.code)}>
                Copiar código
              </button>
              {copyState === "copied" && (
                <p className="notice" role="status">
                  Código copiado.
                </p>
              )}
              {copyState === "failed" && (
                <p className="form-error" role="alert">
                  No se pudo copiar el código. Cópialo a mano.
                </p>
              )}
            </div>
          )}
        </section>
      )}
      {selected && (
        <section className="dni-view" aria-labelledby="dni-heading">
          <h2 id="dni-heading" ref={dniHeadingRef} tabIndex={-1}>
            DNI de {selected.username}
          </h2>
          {dniQuery.isPending && (
            <p className="loading" role="status">
              Cargando DNI…
            </p>
          )}
          {dniFailure && (
            <p className="form-error" role="alert">
              {messageForDniFailure(dniFailure)}
            </p>
          )}
          {dniQuery.data && (
            <>
              <p>
                <strong>Número de DNI:</strong> {dniQuery.data.number}
              </p>
              {frontPhotoQuery.data && backPhotoQuery.data ? (
                <div className="dni-photos">
                  <figure>
                    <PhotoImage
                      blob={frontPhotoQuery.data}
                      alt={`Foto delantera del DNI de ${selected.username}`}
                    />
                    <figcaption>Anverso</figcaption>
                  </figure>
                  <figure>
                    <PhotoImage
                      blob={backPhotoQuery.data}
                      alt={`Foto trasera del DNI de ${selected.username}`}
                    />
                    <figcaption>Reverso</figcaption>
                  </figure>
                </div>
              ) : (
                !dniFailure && (
                  <p className="loading" role="status">
                    Cargando fotos…
                  </p>
                )
              )}
            </>
          )}
        </section>
      )}
      <dialog
        ref={confirmRef}
        className="confirm-dialog"
        aria-labelledby="confirm-heading"
        onClose={handleConfirmClose}
      >
        {confirmation && (
          <>
            <h2 id="confirm-heading">{confirmation.heading}</h2>
            <p>{confirmation.body}</p>
            <div className="confirm-actions">
              <button type="button" onClick={() => confirmRef.current?.close("cancel")}>
                Cancelar
              </button>
              <button
                type="button"
                className="danger"
                onClick={() => confirmRef.current?.close("confirm")}
              >
                {confirmation.confirmLabel}
              </button>
            </div>
          </>
        )}
      </dialog>
    </section>
  );
}
