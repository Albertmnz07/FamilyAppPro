# Containerized deployment

Two self-contained compose files replace the hand-built environment. `compose.dev.yml` runs three services: PostgreSQL, the API (back-end `dev` target, source bind-mounted, `spring-boot:run`) and the Vite dev server. The browser reaches Vite on 5173 and the API on 8070 by absolute URL, using the CORS origin the API already allows; the front-end image's `dev` target exists only for this stack. PostgreSQL data lives in a named volume and DNI photos in the bind-mounted `./data/photos`, so `down`/`up` preserves both.

`compose.prod.yml` runs two services: PostgreSQL and a single back-end `runtime` image that serves the API and the built front-end from one origin on port 8070. There is no production front-end container, reverse proxy or registry. Port 8070 is the canonical deployment port across both stacks and the whole repository. The production build bakes the relative `/api/v1` into the SPA, so one image works from any hostname or LAN device; development keeps `http://localhost:8070/api/v1` because the browser runs on the host.

Production is the "fake production" used to learn deployment on a second PC: clone the repository there and run `docker compose -f compose.prod.yml up -d --build`. It is a trusted-LAN exercise with the default local credentials and no TLS, domains or orchestrator.

**Consequences:** ADR-0006 remains in force — production is still one artifact serving API and UI — and this ADR only amends *how* that artifact is produced and deployed: compose instead of a local toolchain, a relative production API URL, and port 8070 everywhere. The two compose files use distinct project names, so their database volumes stay separate.
