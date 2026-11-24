## Agent skills

### Issue tracker

Issues are tracked in the GitHub repo `Albertmnz07/FamilyAppPro` via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

The default triage label vocabulary is used. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context. Read `GLOSSARY.md` and relevant `docs/adr/` before exploring or designing. See `docs/agents/domain.md` and `CODING_STANDARDS.md`.

## Repository layout

- `apps/backend` — Java 21 + Spring Boot API (`albertmnz07.familyapp`). Maven wrapper lives at `apps/backend/mvnw`; Flyway migrations live at `apps/backend/src/main/resources/db/migration`.
- `apps/frontend` — React + TypeScript + Vite SPA. Package scripts run with `npm --prefix apps/frontend run <script>`.
- Root — `Makefile`, `Dockerfile`, `compose.dev.yml`, `compose.prod.yml`, `.githooks/`, `.github/workflows/`, `db/`, `docs/`, `GLOSSARY.md`, and `CODING_STANDARDS.md`.

## Back-end architecture (`apps/backend`, ADR-0010)

Keep the three hexagonal layers strictly separated (enforced on every test run by `HexagonalArchitectureTest`):

- `domain` (`model`, `valueobject`, `repository`, `exception`): pure Java (`java..` only). Place business invariants on Aggregate Roots (`Persona`, `Family`, `Membership` owning internal `Consent`, `Invite`, `AuthToken`) and Value Objects; reference other aggregates by typed ID Value Objects only; signal domain failures with `DomainException` subclasses.
- `application` (`command`, `query`, `port`): pure Java (`domain` + `java..` only). Implement state changes as `*Command` + `*CommandHandler` and reads as `*Query` + `*QueryHandler` (`PageQuery` / `PagedResult`), talking to external systems exclusively through domain repositories and outbound ports (`PhotoStore`, `PasswordHasher`, `InviteCodeGenerator`, `Clock`) with no mediator bus.
- `infrastructure` (`persistence`, `storage`, `security`, `web`, `config`): Spring Boot, Spring Data JPA (`*JpaEntity` + repository adapters), filesystem/crypto adapters, `UseCaseConfig` transactional bean wiring, and REST controllers/DTOs/`@ActingPersona`/`ApiExceptionHandler`. When an API schema changes, refresh the front-end OpenAPI contract (`npm --prefix apps/frontend run api:refresh`).

## Front-end development (`apps/frontend`)

- Run `modern-web-guidance` first for HTML, CSS, and client-side JS/React UI tasks.
- Read `apps/backend/src/main/java/albertmnz07/familyapp/infrastructure/web` controllers, DTOs, `ApiExceptionHandler`, and `apps/frontend/openapi.json` before integrating with the API.
- Call only endpoints implemented by the back-end (ADR-0005, ADR-0006); back-end contract changes belong in a dedicated ticket under the spec/ticket workflow.
- Implement features incrementally within ticket scope.

## Code style

- Never format code by hand. Spotless (google-java-format, AOSP) formats `apps/backend` and Prettier formats `apps/frontend` TypeScript/TSX/CSS (ADR-0009).
- Run `make format` before committing, or `make format-check` to verify, and `make test` (`make test-backend` + `make check`) to run the suites. The pre-commit hook formats staged files and re-stages them, so the commit is the source of truth for style.
- A fresh clone must run `make setup` once to enable `.githooks/`.
