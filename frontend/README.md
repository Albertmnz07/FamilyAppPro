# FamilyApp — frontend

React + TypeScript + Vite app for the family circuit (spec #14). Spanish UI, mobile-first.

## Commands

| Command | What it does |
| --- | --- |
| `npm run dev` | Dev server on `http://localhost:5173`; talks to `http://localhost:8080/api/v1` by default (ADR-0006). |
| `npm run test` | Vitest + Testing Library suite. |
| `npm run typecheck` | `tsc -b`. |
| `npm run lint` | oxlint. |
| `npm run build` | Typecheck + production build. |
| `npm run api:refresh` | Fetches `/v3/api-docs` from the running backend into `openapi.json` and regenerates `src/api/schema.d.ts` (ADR-0005). Override the source with `API_DOCS_URL=<url>`. |

## Structure

- `src/api/` — the one module that touches the network: typed transport over `fetch` (bearer on authenticated calls), RFC 9457 `ProblemDetail` shaping with `code`/`errors`, Spring `Page` parsing, endpoint functions and the generated types (`schema.d.ts`, committed).
- `src/auth/` — in-memory token store and the global 401 handler (redirect to `/login` with a notice).
- `src/forms/` — accessible error bridge: `aria-invalid` in step with `:user-invalid`, one polite live region per page.
- `src/pages/`, `src/shell/`, `src/routing/` — the five routes, the mobile shell and token-presence route protection.

## Configuration

Copy `.env.example` to `.env` to override `VITE_API_BASE_URL`. The backend must allow the dev origin (`http://localhost:5173`, default in the backend's CORS config).
