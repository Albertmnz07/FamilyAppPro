# Front-end in this repo, API by absolute URL, single-artifact deployment

The React + TypeScript front-end lives in `frontend/` in this repository (single Maven module, no monorepo tooling) and reaches the API by absolute URL in development — `VITE_API_BASE_URL`, defaulting to `http://localhost:8080/api/v1` — trading proxy configuration for one environment variable. In production the built front-end is copied into Spring Boot's static resources, so one artifact serves both and no extra CORS configuration is needed.

**Consequences:** CORS stays limited to `GET`, `POST`, `DELETE` and `OPTIONS` with credentials disabled; hosting the front-end on a separate origin, or ever using `PUT`/`PATCH`, is a backend decision first, not a front-end switch.
