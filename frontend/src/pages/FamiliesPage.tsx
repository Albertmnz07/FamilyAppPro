import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router";
import { createFamily, listMyFamilies } from "../api/families";
import { messageForFailure } from "../api/messages";
import { isApiError, normalizeFieldErrors } from "../api/problem";
import { useUserInvalidBridge } from "../forms/useUserInvalidBridge";

const nameConstraintMessage = "Escribe un nombre de hasta 100 caracteres.";

/** A server-side validation failure on `name` must mark that input, not just the page. */
function rejectsName(error: unknown): boolean {
  if (!isApiError(error)) return false;
  return normalizeFieldErrors(error.problem?.errors ?? []).fieldErrors.some(({ field }) => field === "name");
}

export function FamiliesPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const nameInputRef = useRef<HTMLInputElement | null>(null);
  const formErrorRef = useRef<HTMLParagraphElement | null>(null);
  // One bridge for the whole page: both forms share its single polite live region.
  const pageRef = useUserInvalidBridge();
  const [page, setPage] = useState(0);
  const [formError, setFormError] = useState<string | null>(null);
  const familiesQuery = useQuery({
    queryKey: ["families", page],
    queryFn: ({ signal }) => listMyFamilies(page, signal),
  });
  const pageData = familiesQuery.data;

  const createMutation = useMutation({
    mutationFn: (name: string) => createFamily(name),
    onSuccess: async () => {
      setFormError(null);
      setPage(0);
      await queryClient.invalidateQueries({ queryKey: ["families"] });
    },
    onError: (error) => {
      const input = nameInputRef.current;
      if (rejectsName(error) && input) {
        // Let the browser's validity state mark the field; the bridge then sets
        // aria-invalid, links its Spanish error and lets focus announce it.
        input.setCustomValidity(nameConstraintMessage);
        input.checkValidity();
        input.focus();
        setFormError(null);
      } else {
        input?.setCustomValidity("");
        setFormError(messageForFailure(error));
      }
    },
  });

  useEffect(() => {
    if (formError) formErrorRef.current?.focus();
  }, [formError]);

  function handleRedeem(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const code = String(new FormData(event.currentTarget).get("code") ?? "");
    // Only keep the URL tidy. Uppercasing and normalization before the request
    // belong to the redemption screen (spec #14, "Invite UX").
    navigate(`/invites/${encodeURIComponent(code.trim())}`);
  }

  function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    setFormError(null);
    const name = String(new FormData(form).get("name") ?? "");
    createMutation.mutate(name, { onSuccess: () => form.reset() });
  }

  return (
    <section className="page" ref={pageRef}>
      <h1>Familias</h1>
      {familiesQuery.isPending && <p role="status">Cargando familias…</p>}
      {familiesQuery.isError && (
        <p className="form-error" role="alert">
          {messageForFailure(familiesQuery.error)}
          <button type="button" onClick={() => void familiesQuery.refetch()}>
            Reintentar
          </button>
        </p>
      )}
      {pageData && (
        <>
          {pageData.content.length > 0 ? (
            <ul className="family-list">
              {pageData.content.map((family) => (
                <li key={family.id}>
                  <Link to={`/families/${family.id}`}>{family.name ?? "Sin nombre"}</Link>
                </li>
              ))}
            </ul>
          ) : (
            <div className="empty-state">
              <p>Aún no perteneces a ninguna familia.</p>
              <button type="button" onClick={() => nameInputRef.current?.focus()}>
                Crear una familia
              </button>
              <h2 id="redeem-invite-heading">Canjear una invitación</h2>
              <form onSubmit={handleRedeem} className="redeem-form" aria-labelledby="redeem-invite-heading">
                <div className="field">
                  <label htmlFor="invite-code">Código de invitación</label>
                  <input
                    id="invite-code"
                    name="code"
                    type="text"
                    required
                    aria-invalid={false}
                    data-error="invite-code-error"
                    autoCapitalize="none"
                    autoCorrect="off"
                    spellCheck={false}
                    enterKeyHint="go"
                  />
                  <span id="invite-code-error" className="field-error">
                    Escribe el código de invitación.
                  </span>
                </div>
                <button type="submit">Canjear invitación</button>
              </form>
            </div>
          )}
          {pageData.totalPages > 0 && (
            <nav className="pagination" aria-label="Paginación de familias">
              <button
                type="button"
                disabled={pageData.number <= 0}
                onClick={() => setPage(pageData.number - 1)}
              >
                Anterior
              </button>
              <span>
                Página {pageData.number + 1} de {pageData.totalPages}
              </span>
              <button
                type="button"
                disabled={pageData.number >= pageData.totalPages - 1}
                onClick={() => setPage(pageData.number + 1)}
              >
                Siguiente
              </button>
            </nav>
          )}
        </>
      )}

      <div className="create-family">
        <h2 id="create-family-heading">Nueva familia</h2>
        {formError && (
          <p className="form-error" tabIndex={-1} ref={formErrorRef}>
            {formError}
          </p>
        )}
        <form onSubmit={handleCreate} className="form" aria-labelledby="create-family-heading">
          <div className="field">
            <label htmlFor="family-name">Nombre de la familia</label>
            <input
              id="family-name"
              name="name"
              type="text"
              required
              maxLength={100}
              autoComplete="off"
              ref={nameInputRef}
              data-error="family-name-error"
              onInputCapture={() => nameInputRef.current?.setCustomValidity("")}
            />
            <span id="family-name-error" className="field-error">
              {nameConstraintMessage}
            </span>
          </div>
          <button type="submit" disabled={createMutation.isPending}>
            {createMutation.isPending ? "Creando…" : "Crear familia"}
          </button>
        </form>
      </div>
    </section>
  );
}
