## Agent skills

### Issue tracker

Issues are tracked in the GitHub repo `Albertmnz07/FamilyAppPro` via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

The default triage label vocabulary is used. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context. See `docs/agents/domain.md`.

## Frontend development

- The backend is implemented in Spring Boot.
- The frontend will use React, TypeScript and Vite.
- Preserve the existing backend architecture.
- Use modern-web-guidance for web development tasks.
- Read existing controllers, DTOs and API contracts
  before implementing frontend integrations.
- Reuse the existing specification and ticket workflow.
- Implement features incrementally.
- Do not invent backend endpoints.

## Code style

- Never format code by hand. Spotless (google-java-format, AOSP) formats the
  Java back-end and Prettier formats the front-end TypeScript/TSX/CSS (ADR-0009).
- Run `make format` before committing, or `make format-check` to verify. The
  pre-commit hook formats the staged files and re-stages them, so the commit is
  the source of truth for style.
- A fresh clone must run `make setup` once to enable the repository hooks.
