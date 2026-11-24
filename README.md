# FamilyAppPro

A family app built around the DNI. Each persona shares their DNI entity (number plus photos) with the families they join, for as long as their consent is in force. A Spring Boot API serves a React + TypeScript front-end from the same artifact, so one process runs the whole app on `localhost`.

The user interface is in Spanish.

## Requirements

| Tool | Version | Needed for |
| --- | --- | --- |
| Java | 21 | API (`apps/backend`), build and tests |
| PostgreSQL | any recent version, listening on `localhost:5432` | the database |
| Node.js + npm | 22.12 or newer | building `apps/frontend`, its tests, front-end development |

For the containerized path below, Docker (Engine plus the Compose plugin), `make` and git are enough — no local Java, PostgreSQL or Node.

## Project structure

Application code lives under `apps/`, separated from repository-wide orchestration, hooks, and documentation ([ADR-0010](docs/adr/0010-hexagonal-backend-architecture.md)):

```
FamilyAppPro/
├── apps/
│   ├── backend/         # Spring Boot API (albertmnz07.familyapp), Maven wrapper, Flyway SQL
│   └── frontend/        # React + TypeScript + Vite SPA, OpenAPI snapshot & generated types
├── db/bootstrap.sql     # one-time local PostgreSQL role/database bootstrap
├── docs/                # ADRs (docs/adr/) and Spanish usage guide (docs/usage.md)
├── .githooks/           # pre-commit & pre-push formatting/check hooks (make setup)
├── compose.dev.yml      # dev stack: PostgreSQL + API (spring-boot:run) + Vite (port 5173/8070)
├── compose.prod.yml     # prod stack: PostgreSQL + single-artifact runtime image (port 8070)
├── Dockerfile           # multi-stage back-end/single-artifact image (dev and runtime targets)
└── Makefile             # day-to-day developer commands across both apps and stacks
```

Inside `apps/backend`, `albertmnz07.familyapp` follows a strict Hexagonal Architecture enforced by `HexagonalArchitectureTest`:

- **`domain`** (`model`, `valueobject`, `repository`, `exception`) — framework-free Aggregates (`Persona`, `Family`, `Membership` encapsulating `Consent`, `Invite`, `AuthToken`), Value Objects, repository interfaces, and `DomainException` types.
- **`application`** (`command`, `query`, `port`) — framework-free `*Command`/`*CommandHandler` mutations, `*Query`/`*QueryHandler` reads, and outbound ports (`PhotoStore`, `PasswordHasher`, `InviteCodeGenerator`, `Clock`).
- **`infrastructure`** (`persistence`, `storage`, `security`, `web`, `config`) — JPA entities/adapters, local photo store, PBKDF2/random adapters, transactional bean wiring (`UseCaseConfig`), and REST controllers/DTOs.

## Deploy with Docker

Clone the repository and run one compose file from its root. `.env` is optional: [`.env.example`](.env.example) lists every compose variable and its default, and those defaults start both stacks as-is without committing a secret (`.env` is git-ignored). For a step-by-step guide to running the stacks and using the app, see [docs/usage.md](docs/usage.md).

The everyday commands are wrapped in a [`Makefile`](Makefile): run `make` (or `make help`) to list every target. Each block below shows the `make` target first, then the compose, Maven or npm command it runs underneath.

### Development

```bash
git clone https://github.com/Albertmnz07/FamilyAppPro.git
cd FamilyAppPro
make dev
```

`make dev` runs:

```bash
docker compose -f compose.dev.yml up --build
```

Open <http://localhost:5173> for the app; the API listens on <http://localhost:8070>. The stack is PostgreSQL, the API (source bind-mounted, running `spring-boot:run`) and the Vite dev server. The browser runs on `http://localhost:5173`, one of the CORS origins allowed by default, and calls the API on `localhost:8070`. Front-end edits hot-reload in Vite; after editing Java sources, run `make dev-restart` (`docker compose -f compose.dev.yml restart api`) to recompile. Neither needs an image rebuild, and later runs can start in the background with `make dev-detached` (`docker compose -f compose.dev.yml up -d --build`).

### Production on a second PC (fake production)

```bash
git clone https://github.com/Albertmnz07/FamilyAppPro.git
cd FamilyAppPro
make prod
```

`make prod` rebuilds and redeploys the production services that changed:

```bash
docker compose -f compose.prod.yml up -d --build
```

Repeated deploys recreate only the services whose image or config changed, so containers never pile up. To start the stack without touching the images, `make prod-start` runs `docker compose -f compose.prod.yml up -d --no-build`. To discard the build cache and rebuild the API image from scratch, `make prod-rebuild` runs `docker compose -f compose.prod.yml build --no-cache api` and then `docker compose -f compose.prod.yml up -d`.

Open <http://localhost:8070> on that PC, or `http://<LAN-IP>:8070` from another device on the same LAN (find the address with `hostname -I` on Linux or `ipconfig` on Windows). Only PostgreSQL and one back-end image run: the jar serves the API and the built front-end from the same origin, as [ADR-0006](docs/adr/0006-frontend-location-and-api-base-url.md) requires. The production build bakes the **relative** `/api/v1` into the front-end, so the same image works for any hostname or LAN address without rebuilding; development uses the absolute URL because the browser runs on the host. This is a trusted-LAN deployment exercise: no TLS, registry or non-default credentials.

Both stacks publish port 8070, so stop one before starting the other: `make dev-down` or `make prod-down`, each running `docker compose -f <file> down`. `down` keeps PostgreSQL data in the `pgdata` volume and DNI photos in `./data/photos`. The reset targets — `make db-reset-dev` and `make db-reset-prod` — run `down -v` and bring the stack back up, deleting the database volume while keeping the photos. For a host-run setup without Docker, follow [Deploy it on localhost](#deploy-it-on-localhost) below. The topology and why the two compose files differ are recorded in [ADR-0008](docs/adr/0008-containerized-deployment.md).

## Deploy it on localhost

### 1. Bootstrap the database (one time)

```bash
sudo -u postgres psql -f db/bootstrap.sql
```

This creates the `familyapp` role (`familyapp` / `familyapp`) and the `familyapp` database. Flyway applies the migrations in `apps/backend/src/main/resources/db/migration` on every start, so the schema is never created by hand.

To start over, stop the app, then drop and re-create the database and delete the photo store:

```bash
sudo -u postgres psql -c 'DROP DATABASE familyapp'
sudo -u postgres psql -f db/bootstrap.sql
rm -rf data/photos
```

### 2. Build and run the single artifact

```bash
make run
```

`make run` builds the artifact (`make build`, i.e. `(cd apps/backend && ./mvnw package)`) and starts the jar (`make clean` removes `apps/backend/target` and `apps/frontend/dist`):

```bash
(cd apps/backend && ./mvnw package)
java -jar apps/backend/target/familyapp-0.0.1-SNAPSHOT.jar
```

Then open <http://localhost:8070>.

The `package` phase runs `npm ci` and `npm run build` inside `apps/frontend/` and copies `apps/frontend/dist` into the jar's static resources, so the one process serves both the app and the API at the same origin — no second server and no CORS involved. Building the artifact requires npm on the build machine; `make test-backend` (`(cd apps/backend && ./mvnw test)`) does not.

### Run the front-end in development (optional)

Two processes, with Vite serving the app and proxying nothing — the front-end calls the API by absolute URL:

```bash
# terminal 1
(cd apps/backend && ./mvnw spring-boot:run)

# terminal 2
cd apps/frontend
npm install        # or `npm ci` for a lockfile-exact install
npm run dev
```

Vite serves the app on <http://localhost:5173>, which is one of the CORS origins allowed by default. Set `VITE_API_BASE_URL` before `npm run dev` (or `npm run build`) to point the app at a different API.

## Use it

The whole circuit needs two personas: the one who creates the family, and the one who joins it. Use a second browser profile or a private window for the second persona, so the two sessions don't share a token.

1. **Register** at `/register`. Each persona needs a username (3–30 characters: letters, digits, dots, underscores or hyphens), a DNI number (any non-blank value; it is unique and never edited) and a password of 3–100 characters. No photos at sign-up.
2. **Log in** at `/login`. The session lasts seven days; when the token expires the app returns to the login screen with a notice.
3. **Add your DNI photos** (optional) from the personal menu at `/mi-perfil`, linked from the top bar. It shows your username and DNI read-only, and `Subir fotos del DNI` takes a front and/or back photo from the file picker or the phone camera (JPEG/PNG, up to 10 MB each). You can add or replace a side at any time.
4. **Create a family** on `/families`. Whoever creates it becomes its Admin, marked with a badge in the workspace.
5. **Issue an invite** from the family workspace: `Emitir invitación` shows a single-use code and its expiry (seven days), with a button to copy it.
6. **Redeem the invite** as the second persona: paste the code into `Canjear una invitación` on `/families`, or open the shared link `/invites/<code>` directly. Joining accepts that persona's consent, so their DNI becomes visible to the family.
7. **View a DNI** with `Ver DNI` on a member row: the number and any uploaded photos, while that persona's consent is in force. Tap a photo to enlarge it (close with the cross, a tap outside or Escape); a member who has uploaded none shows `Sin fotos del DNI`.
8. **Revoke your own consent** with `Revocar consentimiento` on your own row, after confirming. The row then reads `Sin consentimiento` and the DNI stops being shown to anyone in that family. You stay a member — revoking consent is not leaving the family.
9. **Remove a member** (Admin only) with `Retirar` on any row except your own, after confirming. The row disappears and that persona loses access to the family. The family's Admin cannot be removed: the API refuses with `cannot-remove-admin`, and the app shows the refusal in Spanish.

## Configuration

Everything has a working default for local use; override with environment variables.

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/familyapp` | JDBC URL |
| `DB_USERNAME` | `familyapp` | database user |
| `DB_PASSWORD` | `familyapp` | database password |
| `PHOTOS_DIR` | `./data/photos` | where DNI photos are stored on disk |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | origins allowed to call the API in development |
| `VITE_API_BASE_URL` | `http://localhost:8070/api/v1` | API base URL compiled into the front-end |

The server listens on port 8070. If you change it with `SERVER_PORT`, rebuild the front-end with a matching `VITE_API_BASE_URL`, because the built app carries the API URL rather than discovering it.

DNI photos live outside PostgreSQL, in the local photo store rooted at `PHOTOS_DIR`; the database keeps only the photo keys. Uploads are capped at 10 MB per photo and 30 MB per request.

## API

- Base path: `http://localhost:8070/api/v1`
- OpenAPI description: `http://localhost:8070/v3/api-docs`
- Swagger UI: `http://localhost:8070/swagger-ui.html`

### Authentication

Login exchanges the credentials for a bearer token:

```bash
curl -X POST http://localhost:8070/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"<username>","password":"<password>"}'
```

Every protected request then carries the token:

```
Authorization: Bearer <token>
```

A missing, malformed or unknown token returns `401`. Tokens expire after seven days.

### Endpoints

| Method | Path | Who |
| --- | --- | --- |
| `POST` | `/api/v1/personas` (multipart: `username`, `dni`, `password`) | anyone |
| `POST` | `/api/v1/auth/login` (`username`, `password`) | anyone |
| `GET` | `/api/v1/personas/me` | authenticated persona |
| `PUT` | `/api/v1/personas/me/dni/photos` (multipart: `frontPhoto`, `backPhoto`, both optional) | authenticated persona |
| `GET` | `/api/v1/personas/me/photos/{front,back}` | authenticated persona |
| `POST` | `/api/v1/families` | authenticated persona |
| `GET` | `/api/v1/me/families` | authenticated persona |
| `GET` | `/api/v1/families/{id}` | accepted member |
| `GET` | `/api/v1/families/{id}/members` | accepted member |
| `POST` | `/api/v1/families/{id}/invites` | admin |
| `POST` | `/api/v1/invites/{code}/redemption` | authenticated persona |
| `DELETE` | `/api/v1/families/{id}/members/{personaId}` | admin |
| `DELETE` | `/api/v1/families/{id}/members/{personaId}/consent` | that persona only |
| `GET` | `/api/v1/families/{id}/members/{personaId}/dni` | accepted member, consent in force |
| `GET` | `/api/v1/families/{id}/members/{personaId}/dni/{front,back}` | accepted member, consent in force |

Members are listed alphabetically by DNI. Each member exposes its `username` (always) and its `dni` (`null` while that persona's consent is not in force). `PersonaResponse` and `DniResponse` expose `frontPhotoUrl`/`backPhotoUrl` as `null` until that side is uploaded. Errors use `application/problem+json` (RFC 9457) with a `code` property, which is what the front-end maps to Spanish messages.

`GET /api/v1/me/families` and `GET /api/v1/families/{id}/members` are paginated with Spring's `page`/`size` query parameters (default `page=0`, `size=20`, maximum `size=100`) and return Spring's paged JSON shape (`content`, `totalElements`, `totalPages`, `number`, `size`, …). A `size` above the maximum is clamped to `100` rather than rejected. Ordering is fixed server-side (families by name, members by DNI): the client `sort` parameter is ignored and the paged metadata reports the page as unsorted.

## Database and migrations

The schema is owned by the ordered migrations in `apps/backend/src/main/resources/db/migration`, applied by Flyway on startup; Hibernate never changes the schema, it only validates the migrated one against the entities (`spring.jpa.hibernate.ddl-auto=validate`).

`V1` captures the schema that `ddl-auto=update` produced (including the legacy `front_photo`/`back_photo` bytea columns); `V2` drops those columns after the move to object storage. A database created before Flyway was adopted is baselined at V1, so it skips `V1` and receives `V2` and every later migration. To change the schema, add a migration — `V3__add_membership_status_index.sql` — and never edit one that has already been applied.

## Front-end

The React + TypeScript + Vite app lives in `apps/frontend/` (see [ADR-0006](docs/adr/0006-frontend-location-and-api-base-url.md) and [apps/frontend/README.md](apps/frontend/README.md)). It reaches the API by absolute URL from `VITE_API_BASE_URL`, defaulting to `http://localhost:8070/api/v1`; there is no dev proxy, so development relies on the CORS origins above.

The production build is integrated into the single Spring artifact: `npm run build` (which runs `tsc -b && vite build`) emits `apps/frontend/dist`, and the Maven `package` phase runs `npm ci` and `npm run build` in `apps/frontend/` and copies `apps/frontend/dist` into the jar's static resources (`apps/backend/target/classes/static`). The npm steps are bound to `prepare-package`, after the test phase, so `(cd apps/backend && ./mvnw test)` never needs Node.

### OpenAPI contract

Front-end API types are generated from the committed OpenAPI snapshot `apps/frontend/openapi.json` into `apps/frontend/src/api/schema.d.ts` ([ADR-0005](docs/adr/0005-openapi-snapshot-as-frontend-contract.md)). After changing a back-end endpoint or DTO, start the API on port 8070 (for example with `make dev` or `(cd apps/backend && ./mvnw spring-boot:run)`) and refresh both files from the repository root:

```bash
npm --prefix apps/frontend run api:refresh
```

Override the spec source URL with `API_DOCS_URL=<url>` if the API is listening elsewhere.

### Checks

Inside `apps/frontend/`, `npm run check` runs typecheck, lint and the test suite in one go; lint treats warnings as failures. From the repository root, `make check` (alias of `make test-frontend`) runs the same command: `npm --prefix apps/frontend run check`.

### Formatting

Code style is owned by the tooling ([ADR-0009](docs/adr/0009-code-formatting-tools.md)), not by hand: Spotless formats `apps/backend` with google-java-format (AOSP style) and Prettier formats `apps/frontend` TypeScript/TSX/CSS. Apply or verify every file with:

```bash
make format        # apply both formatters to apps/backend and apps/frontend
make format-check  # verify without writing (what pre-push and CI run)
```

### Git hooks

The repository ships its git hooks in `.githooks/`: `pre-commit` formats the staged Java (`apps/backend/`) and front-end (`apps/frontend/src/`) files, re-stages them and runs `npm --prefix apps/frontend run check`; `pre-push` runs the strict format checks and `(cd apps/backend && ./mvnw -q test)`. Pull requests and pushes to `main` also run `make format-check` in GitHub Actions (`.github/workflows/format.yml`). Enable the local hooks once per clone:

```bash
make setup
```

## Tests

```bash
make test
```

`make test` runs both suites:

```bash
make test-backend                     # (cd apps/backend && ./mvnw test), on H2 in PostgreSQL mode
make test-frontend                    # npm --prefix apps/frontend run check (typecheck, lint, tests)
```

The backend suite runs pure domain/application unit tests, ArchUnit hexagonal boundary checks, and full-stack `MockMvc` tests applying the same Flyway migrations against H2, so a migration that drifts from the JPA entities fails the suite.

## Known limitations

- **No SPA fallback.** The jar serves the app at `/` and the hashed assets, but reloading a client route such as `/families/{id}` returns 404. Enter the app at `http://localhost:8070` and navigate from there; deep-link forwarding needs a backend route, which the front-end spec deliberately excluded.
- **Invites are single-use and expire after seven days**, and the app shows no invite history.
- **No password reset or account deletion** yet.
- **Photos are local files** under `PHOTOS_DIR`, so they do not travel with the database.
