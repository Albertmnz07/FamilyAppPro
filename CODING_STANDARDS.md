# Coding standards

Rules the review agent enforces. Only **judgement calls** live here: anything a linter, the type checker or the test suite can catch belongs in those tools instead. Where this file endorses something the generic smell baseline would flag, this file wins.

## Scope

- **The ticket's scope is the boundary.** Implement what the ticket and its parent spec ask for; a change outside that surface is a finding even when it looks harmless or adjacent. A backend change on a front-end ticket needs its own ticket.

## Front-end (React + TypeScript)

- **All user-facing copy is Spanish**, and failures are rendered through `messageForFailure` (`src/api/messages.ts`) so a new backend error code gets one mapping rather than a string per page.
- **Pages never call `fetch`.** Every request goes through `src/api/*` on top of `transport.ts`, with types from the OpenAPI snapshot (`src/api/schema.d.ts`, refreshed with `npm run api:refresh`).
- **A paged list's empty state keys on `totalElements`**, never on the current page's rows: removing the last row of a later page must not claim the collection is empty. Clamp `page` to `totalPages - 1` during render, not in an effect.
- **Mutations invalidate the queries whose data they change** (`queryClient.invalidateQueries`), so the UI reflects the new state without a manual reload. Actions that share one error banner share one mutation.
- **Destructive actions confirm through the native `<dialog>`** opened with `showModal()` — the pattern in `FamilyWorkspacePage`, not `window.confirm` and not a two-step inline button.
- **Every list screen has three states**: loading (`role="status"`), empty (`.empty-state` with a next action), and failure (`role="alert"` with `Reintentar`).
- **Row actions carry a distinguishing `aria-label`** (`Retirar a {username}`), so a screen reader announces which row a button belongs to.

## Tests

- **Tests sit beside their subject** (`src/api/members.test.ts`, `src/pages/FamilyWorkspacePage.test.tsx`) and stub the HTTP boundary with `vi.stubGlobal("fetch", …)` plus the helpers in `src/test/http.ts`.
- **Assert what the user sees**: roles, accessible names and visible Spanish text — not class names or internal state.
- **Every refusal path gets a test**, not just the happy path: a mapped error (`forbidden`, `cannot-remove-admin`) is behaviour and must be pinned.
