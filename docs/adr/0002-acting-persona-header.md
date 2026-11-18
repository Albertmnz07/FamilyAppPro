---
status: superseded by ADR-0004
---

# Acting persona identified by header

Until real authentication exists, the API resolves the **acting persona** from the `X-Persona-Id` request header: a UUID looked up in `PersonaRepository` by an argument resolver, surfaced to controllers as an annotated `Persona` parameter. A missing, malformed or unknown UUID is rejected with `401`.

We chose this over JWT, session cookies, or putting the persona in the path because it keeps the token out of every URL, requires no login flow, and concentrates the substitution point in a single resolver. The front-end can start building against the API now, and replacing the header with real authentication later touches the resolver and the request setup, not the domain or the controllers.
