// The bearer token lives in memory only (spec #14): no persistence, no refresh,
// no logout beyond discarding it — the backend offers none. A reload ends the
// session by design; that trade-off was accepted for this cut.
let token: string | null = null;

export function getToken(): string | null {
  return token;
}

export function setToken(value: string): void {
  token = value;
}

export function clearToken(): void {
  token = null;
}
