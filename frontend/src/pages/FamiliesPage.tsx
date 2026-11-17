import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router";
import { createFamily, listMyFamilies } from "../api/families";
import { messageForProblem, networkErrorMessage } from "../api/messages";
import { isApiError } from "../api/problem";
import { useUserInvalidBridge } from "../forms/useUserInvalidBridge";

function failureMessage(error: unknown): string {
  if (isApiError(error)) return messageForProblem(error.problem ?? {});
  return networkErrorMessage;
}

export function FamiliesPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const nameInputRef = useRef<HTMLInputElement | null>(null);
  const formErrorRef = useRef<HTMLParagraphElement | null>(null);
  const formRef = useUserInvalidBridge();
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
    onError: (error) => setFormError(failureMessage(error)),
  });

  useEffect(() => {
    if (formError) formErrorRef.current?.focus();
  }, [formError]);

  function handleRedeem(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const code = String(new FormData(event.currentTarget).get("code") ?? "");
    navigate(`/invites/${encodeURIComponent(code.trim().toUpperCase())}`);
  }

  function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    setFormError(null);
    const name = String(new FormData(form).get("name") ?? "");
    createMutation.mutate(name, { onSuccess: () => form.reset() });
  }

  return (
    <section className="page">
      <h1>Familias</h1>
      {familiesQuery.isPending && <p role="status">Cargando familias…</p>}
      {familiesQuery.isError && (
        <p className="form-error" role="alert">
          {failureMessage(familiesQuery.error)}
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
                    autoCapitalize="none"
                    autoCorrect="off"
                    spellCheck={false}
                    enterKeyHint="go"
                  />
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
        <form ref={formRef} onSubmit={handleCreate} className="form" aria-labelledby="create-family-heading">
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
            />
            <span id="family-name-error" className="field-error">
              Escribe un nombre de hasta 100 caracteres.
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
