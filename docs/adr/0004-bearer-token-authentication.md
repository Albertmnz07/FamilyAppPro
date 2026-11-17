# Acting persona authenticated with opaque bearer tokens

ADR-0002's `X-Persona-Id` header was always a local placeholder, and ticket TN-07 replaced it with real authentication: a Persona logs in with Username and password and receives an opaque token stored server-side in `auth_token` with a seven-day TTL. Every request carries `Authorization: Bearer <token>`, resolved by the same argument-resolver seam, so the substitution point ADR-0002 deliberately concentrated stayed put and the domain and controllers were untouched.

**Consequences:** there is no logout, refresh or token-revocation endpoint and `LoginResponse` does not expose the expiry; unknown and expired tokens are indistinguishable (`401` with `code=unauthorized`). Clients must treat `401` as "session over" and decide session persistence locally.
