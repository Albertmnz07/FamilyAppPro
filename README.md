# FamilyAppPro

Local API for the family app: personas identified by DNI share their DNI entity (number plus photos) with the families they join, while consent is in force.

## Requirements

- Java 21
- PostgreSQL running locally on `localhost:5432`

## Bootstrap the database (one time)

```bash
sudo -u postgres psql -f db/bootstrap.sql
```

The application expects database `familyapp` with user `familyapp` / password `familyapp`, and applies the versioned Flyway migrations in `src/main/resources/db/migration` on startup (`spring.jpa.hibernate.ddl-auto=validate` checks the schema against the entities). A schema created earlier by `ddl-auto=update` is baselined automatically at V1. Override with `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` if needed.

## Schema migrations

The schema is owned by the ordered migrations in `src/main/resources/db/migration`, applied by Flyway on startup; Hibernate never changes the schema, it only validates the migrated one against the entities.

`V1` captures the schema that `ddl-auto=update` produced (including the legacy `front_photo`/`back_photo` bytea columns); `V2` drops those columns after the move to object storage. A database created before Flyway was adopted is baselined at V1, so it skips `V1` and receives `V2` and every later migration. To change the schema, add a migration — `V3__add_membership_status_index.sql` — and never edit one that has already been applied.

The tests run the same migrations against H2 in PostgreSQL mode, so a migration that drifts from the entities fails the suite.

## Run

```bash
./mvnw spring-boot:run
```

- API: `http://localhost:8080/api/v1`
- OpenAPI description: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

DNI photos are stored outside PostgreSQL, in the local photo store rooted at `./data/photos` (override with `PHOTOS_DIR`); the database keeps only the photo keys.

## Authentication

A persona registers with a `username`, a `dni`, a `password` (8-100 characters) and the front/back photos. Login exchanges the credentials for a bearer token:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"<username>","password":"<password>"}'
```

Every protected request then carries the token:

```
Authorization: Bearer <token>
```

A missing, malformed or unknown token returns `401`. Tokens expire after seven days. CORS for the front-end defaults to `http://localhost:3000` and `http://localhost:5173` (override with `CORS_ALLOWED_ORIGINS`).

## Endpoints

| Method | Path | Who |
| --- | --- | --- |
| `POST` | `/api/v1/personas` (multipart: `username`, `dni`, `password`, `frontPhoto`, `backPhoto`) | anyone |
| `POST` | `/api/v1/auth/login` (`username`, `password`) | anyone |
| `GET` | `/api/v1/personas/me` | authenticated persona |
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

Members are listed alphabetically by DNI. Each member exposes its `username` (always) and its `dni` (`null` while that persona's consent is not in force). Errors use `application/problem+json` (RFC 9457) with a `code` property.
