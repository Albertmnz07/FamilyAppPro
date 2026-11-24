import { useMutation } from "@tanstack/react-query";
import { useEffect, useRef } from "react";
import { Link, useParams } from "react-router";
import { normalizeInviteCode, redeemInvite } from "../api/invites";
import { messageForFailure } from "../api/messages";

const missingCodeMessage = "El enlace no incluye ningún código de invitación.";

/**
 * Redeeming is one step: reaching `/invites/:code` (with a code pasted on the
 * Families screen or a shared link) fires the single request, normalized to
 * uppercase without surrounding spaces. There is no Invite history or listing
 * to show — only the outcome (spec #14, "Invite UX").
 */
export function InviteRedemptionPage() {
  const { code = "" } = useParams<{ code: string }>();
  const inviteCode = normalizeInviteCode(code);
  const { mutate, isPending, isError, isSuccess, error, data } = useMutation({
    mutationFn: (pastedCode: string) => redeemInvite(pastedCode),
  });
  const requested = useRef<string | null>(null);

  useEffect(() => {
    // One request per code. The ref also absorbs StrictMode's double mount,
    // which would otherwise make the single-use code look already redeemed.
    if (inviteCode === "" || requested.current === code) return;
    requested.current = code;
    mutate(code);
  }, [code, inviteCode, mutate]);

  return (
    <section className="page">
      <h1>Canjear invitación</h1>
      {inviteCode === "" ? (
        <p className="form-error" role="alert">
          {missingCodeMessage}
        </p>
      ) : (
        <>
          {isPending && (
            <p className="loading" role="status">
              Canjeando la invitación…
            </p>
          )}
          {isError && (
            <p className="form-error" role="alert">
              {messageForFailure(error)}
              <button type="button" onClick={() => mutate(code)}>
                Reintentar
              </button>
            </p>
          )}
          {isSuccess && (
            <>
              <p className="notice" role="status">
                Te has unido a la familia.
              </p>
              <ul className="family-list">
                <li>
                  <Link to={`/families/${data.familyId}`}>Abrir la familia</Link>
                </li>
                <li>
                  <Link to="/families">Ver mis familias</Link>
                </li>
              </ul>
            </>
          )}
        </>
      )}
    </section>
  );
}
