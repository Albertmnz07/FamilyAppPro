# OpenAPI snapshot as the front-end contract

The API contract exists only at runtime (`/v3/api-docs` via springdoc), so a front-end build has nothing checked in to generate types from, and types written by hand from the DTOs would drift with every backend change. We version a snapshot of the runtime spec in the repository and generate the front-end's API types from it; the running backend stays the source of truth, and refreshing the snapshot turns contract drift into a reviewable diff.

**Considered options:** hand-written types (drift with every controller change) and runtime codegen against a live backend (builds would require a running server). The snapshot keeps generation deterministic and the drift explicit.
