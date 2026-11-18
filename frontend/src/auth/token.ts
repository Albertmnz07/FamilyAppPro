// The session lives in memory only (spec #14): the bearer token and the Persona
// it identifies. No persistence, no refresh, no logout beyond discarding it —
// the backend offers none. A reload ends the session by design; that trade-off
// was accepted for this cut.
let token: string | null = null;
let personaId: string | null = null;

export function getToken(): string | null {
  return token;
}

export function setToken(value: string): void {
  token = value;
}

/**
 * The Persona the token belongs to, taken from the login response. The
 * workspace compares it with a Family's admin to decide who may issue Invites.
 */
export function getPersonaId(): string | null {
  return personaId;
}

export function setPersonaId(value: string): void {
  personaId = value;
}

export function clearToken(): void {
  token = null;
  personaId = null;
}
