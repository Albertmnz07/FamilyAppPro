# FamilyAppPro

Local API for the family app: personas identified by DNI share their DNI entity (number plus photos) with the families they join, while consent is in force.

## Requirements

- Java 21
- PostgreSQL running locally on `localhost:5432`

## Bootstrap the database (one time)

```bash
sudo -u postgres psql -f db/bootstrap.sql
```

The application expects database `familyapp` with user `familyapp` / password `familyapp`, and creates the schema on startup (`spring.jpa.hibernate.ddl-auto=update`). Override with `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` if needed.

## Run

```bash
./mvnw spring-boot:run
```

- API: `http://localhost:8080/api/v1`
- OpenAPI description: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Acting persona

There is no authentication yet. Every request identifies the acting persona with the header:

```
X-Persona-Id: <persona-uuid>
```

A missing, malformed or unknown value returns `401`. CORS for the future front-end defaults to `http://localhost:3000` and `http://localhost:5173` (override with `CORS_ALLOWED_ORIGINS`).

## Endpoints

| Method | Path | Who |
| --- | --- | --- |
| `POST` | `/api/v1/personas` (multipart: `username`, `dni`, `frontPhoto`, `backPhoto`) | anyone |
| `GET` | `/api/v1/personas/me` | acting persona |
| `GET` | `/api/v1/personas/me/photos/{front,back}` | acting persona |
| `POST` | `/api/v1/families` | acting persona |
| `GET` | `/api/v1/me/families` | acting persona |
| `GET` | `/api/v1/families/{id}` | accepted member |
| `GET` | `/api/v1/families/{id}/members` | accepted member |
| `POST` | `/api/v1/families/{id}/invites` | admin |
| `POST` | `/api/v1/invites/{code}/redemption` | any registered persona |
| `DELETE` | `/api/v1/families/{id}/members/{personaId}` | admin |
| `DELETE` | `/api/v1/families/{id}/members/{personaId}/consent` | that persona only |
| `GET` | `/api/v1/families/{id}/members/{personaId}/dni` | accepted member, consent in force |
| `GET` | `/api/v1/families/{id}/members/{personaId}/dni/{front,back}` | accepted member, consent in force |

Members are listed alphabetically by DNI. Each member exposes its `username` (always) and its `dni` (`null` while that persona's consent is not in force). Errors use `application/problem+json` (RFC 9457) with a `code` property.
