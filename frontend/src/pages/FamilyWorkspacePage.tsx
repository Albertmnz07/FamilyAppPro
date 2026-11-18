import { useMutation, useQuery } from "@tanstack/react-query";
import { useEffect, useRef, useState } from "react";
import { useParams } from "react-router";
import { getFamily } from "../api/families";
import { issueInvite, type IssuedInvite } from "../api/invites";
import { getDniPhoto, getMemberDni, listFamilyMembers, type Member } from "../api/members";
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

export function FamilyWorkspacePage() {
  const { familyId = "" } = useParams<{ familyId: string }>();
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<Member | null>(null);
  const [invite, setInvite] = useState<IssuedInvite | null>(null);
  const [copyState, setCopyState] = useState<"idle" | "copied" | "failed">("idle");
  const currentPersonaId = getPersonaId();
  const dniHeadingRef = useRef<HTMLHeadingElement | null>(null);
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

  useEffect(() => {
    if (selected) dniHeadingRef.current?.focus();
  }, [selected]);

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
      {membersQuery.isPending && <p className="loading">Cargando miembros…</p>}
      {membersQuery.isError && (
        <p className="form-error" role="alert">
          {messageForFailure(membersQuery.error)}
          <button type="button" onClick={() => void membersQuery.refetch()}>
            Reintentar
          </button>
        </p>
      )}
      {pageData && (
        <>
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
                      {member.consentInForce && (
                        <button
                          type="button"
                          aria-label={`Ver DNI de ${member.username}`}
                          onClick={() => setSelected(member)}
                        >
                          Ver DNI
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
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
          {dniQuery.isPending && <p className="loading">Cargando DNI…</p>}
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
                !dniFailure && <p className="loading">Cargando fotos…</p>
              )}
            </>
          )}
        </section>
      )}
    </section>
  );
}
