# Coding standards

Rules the review agent enforces. Only **judgement calls** live here: anything a linter, the type checker, ArchUnit or the test suite can catch belongs in those tools instead. Where this file endorses something the generic smell baseline would flag, this file wins.

## Scope & layout

- **The ticket's scope is the boundary.** Implement what the ticket and its parent spec ask for; a change outside that surface is a finding even when it looks harmless or adjacent. A back-end change on a front-end ticket needs its own ticket.
- **Applications live under `apps/`.** Back-end code belongs in `apps/backend` (`albertmnz07.familyapp`) and front-end code in `apps/frontend`; repository-wide orchestration (`Makefile`, `Dockerfile`, `compose.*.yml`, `.githooks/`, `docs/`, `GLOSSARY.md`) stays at the root (ADR-0010).

## Back-end (`apps/backend`, Java + Spring Boot — ADR-0010)

- **Business rules live in `domain`, orchestration in `application`, frameworks in `infrastructure`.** `HexagonalArchitectureTest` enforces import purity (`domain` → `java..`; `application` → `domain` + `java..`), and review enforces semantic purity: keep SQL/JPA shapes (`*JpaEntity`), HTTP/Servlet concerns, Bean Validation (`@Valid`), Spring `Page`, and `TransactionTemplate` wiring (`UseCaseConfig`) strictly inside `infrastructure`.
- **Aggregates own invariants and reference other aggregates by typed ID Value Objects.** `Persona`, `Family`, `Membership` (encapsulating `Consent` as an internal Entity so membership lifecycle and consent state never drift), `Invite`, and `AuthToken` validate every rule-bearing property through dedicated Value Objects (`Username`, `DniNumber`, `Dni`, `RawPhoto`, `RawPassword`, `FamilyName`, `InviteCode`, …) rather than primitives or service-layer `if` chains.
- **Separate Commands from Queries with direct handler injection.** Every mutation is a `*Command` + `*CommandHandler` wired transactionally in `UseCaseConfig`, and every read is a `*Query` + `*QueryHandler` wired `readOnly` returning pure `*View` / `PagedResult<T>` records — never introduce a `CommandBus`/`QueryBus` mediator or `@Service`/`@Transactional` annotations in `application`.
- **Domain errors map once at the HTTP edge.** Throw `DomainException` subclasses from `domain` and `application` and translate them in `ApiExceptionHandler` to the twelve RFC 9457 `ProblemDetail.code` values. Preserve lookup error semantics expected by the UI (malformed username on login surfaces as `401 unauthorized`; malformed invite code on redemption surfaces as `404 not-found`), wrap `PagedResult<T>` through `PageResponses` so Spring's paged JSON envelope stays intact, and refresh `apps/frontend/openapi.json` + `apps/frontend/src/api/schema.d.ts` whenever the OpenAPI surface changes (ADR-0005).

## Front-end (`apps/frontend`, React + TypeScript)

- **All user-facing copy is Spanish**, and failures are rendered through `messageForFailure` (`apps/frontend/src/api/messages.ts`) so a new backend error code gets one mapping rather than a string per page.
- **Pages never call `fetch`.** Every request goes through `apps/frontend/src/api/*` on top of `transport.ts`, with types from the OpenAPI snapshot (`apps/frontend/src/api/schema.d.ts`, refreshed with `npm --prefix apps/frontend run api:refresh`).
- **A paged list's empty state keys on `totalElements`**, never on the current page's rows: removing the last row of a later page must not claim the collection is empty. Clamp `page` to `totalPages - 1` during render, not in an effect.
- **Mutations invalidate the queries whose data they change** (`queryClient.invalidateQueries`), so the UI reflects the new state without a manual reload. Actions that share one error banner share one mutation.
- **Destructive actions confirm through the native `<dialog>`** opened with `showModal()` — the pattern in `FamilyWorkspacePage`, not `window.confirm` and not a two-step inline button.
- **Every list screen has three states**: loading (`role="status"`), empty (`.empty-state` with a next action), and failure (`role="alert"` with `Reintentar`).
- **Row actions carry a distinguishing `aria-label`** (`Retirar a {username}`), so a screen reader announces which row a button belongs to.

## Formatting

- **Style is owned by the tools, never by hand.** Spotless formats `apps/backend` with google-java-format (AOSP) and Prettier formats `apps/frontend` TypeScript/TSX/CSS. Run `make format` to apply them or `make format-check` to verify; `.githooks/pre-commit` applies them to staged files and CI verifies with `make format-check` (ADR-0009).

## Tests

- **Back-end tests pin observable seams, never persistence internals.** Exercise Value Objects, Aggregates, and use-case handlers with fast zero-Spring JUnit tests, HTTP contracts and `ProblemDetail.code` mappings end to end through `MockMvc` + H2 + Flyway (`ApiIntegrationTest`), and concurrency/migration/storage guarantees with dedicated adapter contract tests — never test private `*JpaEntity` mappers or mock call counts.
- **Front-end tests sit beside their subject** (`apps/frontend/src/api/members.test.ts`, `apps/frontend/src/pages/FamilyWorkspacePage.test.tsx`), stub the HTTP boundary with `vi.stubGlobal("fetch", …)` plus the helpers in `apps/frontend/src/test/http.ts`, and **assert what the user sees**: roles, accessible names and visible Spanish text — not class names or internal state.
- **Every refusal path gets a test**, not just the happy path: a mapped error (`forbidden`, `cannot-remove-admin`) is behaviour and must be pinned.
