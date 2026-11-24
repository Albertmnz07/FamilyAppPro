// The global 401 handler (spec #14, "Session"): any authenticated call answered
// with 401 means the token expired or is unknown — discard it and land on
// /login with a notice. A 401 while no token is held (wrong credentials on the
// login screen itself) is the caller's business, not a session loss.
import { isApiError } from "../api/problem";
import { clearToken, getToken } from "./token";

export const SESSION_EXPIRED_NOTICE = "Tu sesión ha finalizado. Vuelve a iniciar sesión.";

type UnauthorizedHandler = () => void;

let handler: UnauthorizedHandler | null = null;

/** Registers the redirect side effect; returns its unregistration. */
export function onUnauthorized(register: UnauthorizedHandler): () => void {
  handler = register;
  return () => {
    if (handler === register) handler = null;
  };
}

export function handleUnauthorizedResponse(error: unknown): void {
  if (!isApiError(error) || error.status !== 401 || !getToken()) return;
  clearToken();
  handler?.();
}
