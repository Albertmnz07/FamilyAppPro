# FamilyApp — frontend (`apps/frontend`)

React + TypeScript + Vite app for the family circuit, housed at `apps/frontend` alongside the Spring Boot API in `apps/backend` ([ADR-0006](../../docs/adr/0006-frontend-location-and-api-base-url.md), [ADR-0010](../../docs/adr/0010-hexagonal-backend-architecture.md)). Spanish UI, mobile-first. In production, Maven (`apps/backend/pom.xml`) builds `apps/frontend/dist` and bundles it into the back-end JAR's static resources.

## Commands

Run from `apps/frontend/` (or from the repository root with `npm --prefix apps/frontend run <script>` or the root `Makefile`):

| Command (`apps/frontend`) | Root shortcut | What it does |
| --- | --- | --- |
| `npm run dev` | `make dev` (containerized) | Dev server on `http://localhost:5173`; talks to `http://localhost:8070/api/v1` by default ([ADR-0006](../../docs/adr/0006-frontend-location-and-api-base-url.md)). |
| `npm run check` | `make check` / `make test-frontend` | Runs `typecheck`, `lint` (`--deny-warnings`), and `test` in one pass. |
| `npm run test` | — | Vitest + Testing Library suite. |
| `npm run typecheck` | — | `tsc -b`. |
| `npm run lint` | — | oxlint (`--deny-warnings`). |
| `npm run format` | `make format` | Prettier write over `src/**/*.{ts,tsx,css}` and `vite.config.ts` ([ADR-0009](../../docs/adr/0009-code-formatting-tools.md)). |
| `npm run format:check` | `make format-check` | Prettier check without writing. |
| `npm run build` | `make build` (into JAR) | Typecheck + production Vite bundle into `apps/frontend/dist`. |
| `npm run api:refresh` | `npm --prefix apps/frontend run api:refresh` | Fetches `/v3/api-docs` from the running backend (`http://localhost:8070/v3/api-docs` by default; override with `API_DOCS_URL=<url>`) into `openapi.json` (`api:snapshot`) and regenerates `src/api/schema.d.ts` (`api:types`) ([ADR-0005](../../docs/adr/0005-openapi-snapshot-as-frontend-contract.md)). |

## Structure

- `openapi.json` & `scripts/fetch-openapi.mjs` — versioned snapshot of `/v3/api-docs` and the refresh script that fetches it from a running `apps/backend` instance.
- `src/api/` — the one module that touches the network: typed transport over `fetch` (bearer on authenticated calls), RFC 9457 `ProblemDetail` shaping with `code`/`errors`, Spring `Page` parsing, Spanish failure mapping (`messages.ts`), endpoint functions, and generated OpenAPI types (`schema.d.ts`, committed and excluded from Prettier).
- `src/auth/` — in-memory session (the bearer token and the Persona it identifies, taken from login) and the global 401 handler (redirect to `/login` with a notice).
- `src/forms/` — accessible error bridge: `aria-invalid` in step with `:user-invalid`, one polite live region per page.
- `src/pages/`, `src/shell/`, `src/routing/` — the routes (`/login`, `/register`, `/families`, `/families/:familyId`, `/invites/:code`, `/mi-perfil`), the mobile shell, and token-presence route protection.

## Configuration

Copy `apps/frontend/.env.example` to `apps/frontend/.env` to override `VITE_API_BASE_URL` for host-run Vite (`compose.dev.yml` reads the root `.env` instead). The backend in `apps/backend` allows `http://localhost:5173` by default in its CORS configuration.
