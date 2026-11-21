# Frontend React + TypeScript + Vite — contexto para `grill-with-docs`

> **Estado**: solo investigación. No se ha creado ni modificado código de aplicación.
> **Uso**: este archivo es material de entrada para una sesión `/grill-with-docs`.
> Contiene **hechos verificados en el código** (no hay que preguntarlos al usuario) y un
> **árbol de decisiones** con una frontera inicial propuesta (sí hay que decidirlos).

---

## 0. Cómo encaja la sesión

`/grill-with-docs` no es una skill autoinvocable: se lanza manualmente y compone dos skills:

| Skill | Qué aporta a la sesión |
| --- | --- |
| `grilling` | Entrevista por rondas sobre un **árbol de decisiones**. La *frontera* son las preguntas cuyos prerrequisitos ya están resueltos. Los hechos los busca el agente; las decisiones las toma el usuario. |
| `domain-modeling` | Reta el lenguaje contra `GLOSSARY.md`, propone términos canónicos y escribe ADRs **solo** si son difíciles de revertir, sorprendentes y fruto de un trade-off real. |

Precondición ya satisfecha en este repo: `AGENTS.md`, `GLOSSARY.md` (single-context), `docs/adr/0001..0003`, `docs/agents/{domain,issue-tracker,triage-labels}.md` y tracker GitHub `Albertmnz07/FamilyAppPro` vía `gh`.

Regla para la sesión: **no volver a preguntar lo que ya está en la sección 2**. Preguntar solo lo de la sección 4. No se actúa hasta que el usuario confirme que se alcanzó entendimiento compartido.

---

## 1. Skills revisadas

### 1.1 Ecosistema Matt Pocock (instalado en `.agents/skills/`)

`ask-matt` (router) → **`grill-with-docs`** → (si una pregunta necesita ver estado/UI: `handoff` → `prototype` → `handoff`) → `to-spec` → `to-tickets` → `implement` / `implement-spec` → `tdd` + `code-review` → `pr` → `retro`.

Relevantes para este trabajo:

- **`grill-with-docs`**: entrada correcta porque hay directorio de trabajo; deja rastro en `GLOSSARY.md` y `docs/adr/`.
- **`grilling`** y **`domain-modeling`**: las dos primitives que compone.
- **`migrate-to-shoehorn`**: para los tests del frontend, cuando se necesiten fixtures parciales; **solo en tests**, nunca en producción (`fromPartial`, `fromAny`, `fromExact`).
- **`research`**: opcional; ya se hizo el equivalente con un subagente de exploración.
- **`to-spec` / `to-tickets`**: convertir el resultado de la entrevista en spec + tickets con aristas bloqueantes, respetando el flujo ya usado por el backend (specs #1/#6 y TN-01..TN-10 cerrados).

### 1.2 Modern Web Guidance

Búsquedas ejecutadas (nunca se escribió código, así que las guías no se "aplicaron" aún, solo se inventariaron):

| Consulta | IDs útiles encontrados |
| --- | --- |
| `fetch data` | `js`, `security`, `forms`, `deprioritize-background-fetches` |
| `accessible form validation` | `forms`, `validate-input-after-interaction`, `accessible-error-announcement`, `accessibility`, `autofill-sign-up-form` |
| `responsive data table` | `responsive-table`, `sortable-table`, `responsive-design`, `css-layout`, `accessibility` |

Guías recuperadas en esta sesión y sus reglas a respetar al implementar:

- **`accessible-error-announcement`**: «Set `aria-invalid="false"` until `:user-invalid` matches, rather than leaving the attribute unset.» y «Don't reference the error from `aria-describedby` in the markup: text that `aria-describedby` references is announced even when it's hidden». Además: un único live region `polite` por página y mover el foco al primer campo inválido tras un submit fallido.
- **`deprioritize-background-fetches`**: «Add the `priority: 'low'` option to the `fetch()` initialization object.»; «DO NOT use the deprecated `importance` key». Aplica a prefetch/telemetría, nunca a datos de la vista.

Guías a recuperar **antes de escribir** la UI: `forms`, `accessible-error-announcement`, `responsive-table`, `autofill-sign-up-form`, `security`, `js`, `accessibility`.

---

## 2. Backend: lo que ya existe (hechos)

### 2.1 Stack y ejecución

- Maven, un solo módulo, Java 21, Spring Boot 4.1.2. Sin `spring-boot-starter-security`.
- `springdoc-openapi` 3.2.0 → OpenAPI en `http://localhost:8070/v3/api-docs`, Swagger UI en `/swagger-ui.html`.
- PostgreSQL `jdbc:postgresql://localhost:5432/familyapp` (`familyapp`/`familyapp`), Flyway + Hibernate `validate`.
- Backend en `http://localhost:8070`; prefijo `/api/v1`.
- CORS (`WebConfig.java:31-37`): `GET, POST, DELETE, OPTIONS`; orígenes `http://localhost:3000,http://localhost:5173` (env `CORS_ALLOWED_ORIGINS`); `allowedHeaders("*")`; **`allowCredentials=false`**; sin headers expuestos.
- No existe scaffold frontend: sin `package.json`, `tsconfig`, `vite.config`, ni `.ts/.tsx`. `src/main/resources/static/` y `templates/` están vacíos. No hay frontend en otras ramas ni en el historial.

### 2.2 Endpoints (16 en total, 4 controllers)

**Auth**

| Método y ruta | Request | Response | Notas |
| --- | --- | --- | --- |
| `POST /api/v1/auth/login` | `LoginRequest` JSON | `LoginResponse` | Público. 200. |

**Personas**

| Método y ruta | Request | Response | Notas |
| --- | --- | --- | --- |
| `POST /api/v1/personas` | `multipart/form-data`: `username`, `dni`, `password` (campos) + `frontPhoto`, `backPhoto` (archivos) | `PersonaResponse` | Público. 201. |
| `GET /api/v1/personas/me` | — | `PersonaResponse` | Bearer. |
| `GET /api/v1/personas/me/photos/front` | — | `byte[]` + MIME | Bearer. `Cache-Control: no-store`. |
| `GET /api/v1/personas/me/photos/back` | — | `byte[]` + MIME | Bearer. `Cache-Control: no-store`. |

**Familias**

| Método y ruta | Request | Response | Notas |
| --- | --- | --- | --- |
| `POST /api/v1/families` | `CreateFamilyRequest` | `FamilyResponse` | 201. Creador = admin + miembro aceptado con consentimiento. |
| `GET /api/v1/me/families` | `page`, `size` | `Page<FamilyResponse>` | Orden fijo por nombre (case-insensitive, nulls last). |
| `GET /api/v1/families/{familyId}` | — | `FamilyResponse` | Requiere miembro aceptado. |
| `GET /api/v1/families/{familyId}/members` | `page`, `size` | `Page<MemberResponse>` | Orden fijo por DNI; `dni=null` sin consentimiento. |
| `POST /api/v1/families/{familyId}/invites` | — | `InviteResponse` | Solo admin. 201. Código de 6 chars, 7 días, un solo uso. |
| `DELETE /api/v1/families/{familyId}/members/{personaId}` | — | 204 | Solo admin. El admin no puede ser removido. |
| `DELETE /api/v1/families/{familyId}/members/{personaId}/consent` | — | 204 | Solo uno mismo. Revoca consentimiento. |
| `GET /api/v1/families/{familyId}/members/{personaId}/dni` | — | `DniResponse` | Requiere consentimiento del objetivo. |
| `GET /api/v1/families/{familyId}/members/{personaId}/dni/front` | — | `byte[]` + MIME | Ídem. `no-store`. |
| `GET /api/v1/families/{familyId}/members/{personaId}/dni/back` | — | `byte[]` + MIME | Ídem. `no-store`. |

**Invitaciones**

| Método y ruta | Request | Response | Notas |
| --- | --- | --- | --- |
| `POST /api/v1/invites/{code}/redemption` | — | `RedemptionResponse` | Crea membership + consentimiento. Código case-insensitive. |

No hay `PUT`, `PATCH`, búsqueda, listado de invitaciones, logout, refresh, ni endpoints de health.

### 2.3 DTOs (todos `record`; sin anotaciones Jackson)

| DTO | Campos |
| --- | --- |
| `LoginRequest` | `username`, `password` (`@NotBlank`) |
| `LoginResponse` | `token`, `personaId`, `username` |
| `CreateFamilyRequest` | `name` (`@NotBlank @Size(max=100)`) |
| `FamilyResponse` | `id`, `name`, `adminPersonaId` |
| `MemberResponse` | `personaId`, `username`, `dni`, `admin`, `status`, `consentInForce` |
| `InviteResponse` | `code`, `familyId`, `expiresAt: Instant` |
| `RedemptionResponse` | `familyId`, `personaId`, `status` |
| `DniResponse` | `personaId`, `number`, `frontPhotoUrl`, `backPhotoUrl` |
| `PersonaResponse` | `id`, `username`, `dni`, `frontPhotoUrl`, `backPhotoUrl` |

`PersonaResponse` siempre devuelve las URLs fijas `/api/v1/personas/me/photos/front|back`. `DniResponse` devuelve URLs relativas a la familia. `Family.name` es nullable en BD/entidad por datos legacy.

### 2.4 Autenticación

- Token **opaco** (no JWT), guardado en `auth_token`, TTL **7 días**. Sin `expiresAt` en `LoginResponse`, sin refresh ni revocación.
- Header: `Authorization: Bearer <token>` (prefijo case-insensitive). Resuelto por `ActingPersonaArgumentResolver`.
- Token ausente/malformado/desconocido/expirado → `401` con `code=unauthorized` (no se distingue expirado de inválido).
- Registro y login son los únicos endpoints públicos. El resto exige bearer.
- Autorización imperativa en servicios: admin para invitar/remover, uno mismo para revocar consentimiento, miembro aceptado para ver DNI.

### 2.5 Errores

`ApiExceptionHandler` → `application/problem+json` (RFC 9457) con `title`, `status`, `detail` y `code`:

`unauthorized` (401) · `forbidden` (403) · `not-found` (404) · `duplicate-dni`, `duplicate-username`, `invite-expired`, `invite-already-redeemed`, `cannot-remove-admin` (409) · `unsupported-photo-type` (415) · `photo-too-large` (413) · `validation-failed` (400, con `errors: [{field,message}]`) · `bad-request` (400).

Los nombres de campo de validación pueden venir calificados (`register.username`) o como `"request"` en parámetros de método.

### 2.6 Paginación

Forma `Page` de Spring Data: `content[]`, `pageable`, `last`, `totalElements`, `totalPages`, `number`, `size`, `sort`, `first`, `empty`. `page` base 0, `size` por defecto 20 y **silent clamp a 100**. El parámetro `sort` del cliente se ignora; el servidor fija el orden.

### 2.7 Fotos y privacidad

- Solo `image/jpeg` / `image/png`, ≤ 1 MB por archivo, ≤ 3 MB por request.
- Las imágenes se sirven como bytes con `Cache-Control: no-store`, nunca como archivos estáticos.
- Consentimiento **por familia**, denegado por defecto, revocable por uno mismo; solo se recupera con un **nuevo invite** (no hay endpoint de grant directo).
- Sin consentimiento: `dni=null` en la lista, `403` en detalle/fotos. Miembro ajeno: `403` si quien mira no es miembro; `404` si el objetivo no es miembro.
- En la práctica `status` siempre es `"ACCEPTED"` en la API (los removidos no se listan).

### 2.8 Documentación existente y deriva detectada

- `README.md`: setup, curl de login, tabla de endpoints, reglas de DNI y paginación. Es fuente fiable.
- `GLOSSARY.md`: **Persona** (no User/Account), **Username** (no Nick), **Family** (no Group), **Admin**, **Membership**, **Consent**, **Visibility**, **Invite**. Vocabulario canónico para la sesión.
- ADR-0001 (consentimiento por par persona-familia) y ADR-0003 (Flyway) vigentes.
- **ADR-0002 está obsoleto**: dice que la identidad viene de `X-Persona-Id` y que "no hay login"; el código usa bearer desde TN-07. El spec GitHub #6 también está desactualizado (decía "no pagination"). README + código son la verdad; la sesión debe decidir cómo documentar la corrección.
- OpenAPI solo en runtime: **no hay spec versionada en el repo**, así que un build frontend no puede generar tipos desde un archivo checked-in sin snapshot previo.
- `ApiIntegrationTest` (33 tests, H2 + Flyway reales) es la referencia ejecutable del contrato: registro/validación, auth, familias, paging, admin-only, invitaciones, matriz de visibilidad, consentimiento, OpenAPI y CORS preflight.

---

## 3. Restricciones duras (no son decisiones de producto)

1. El frontend **no puede inventar endpoints** (`AGENTS.md`); cualquier operación nueva requiere backend + CORS.
2. No hay edición ni borrado de familias/personas: las pantallas no deben ofrecerlos.
3. No hay logout/refresh: el frontend solo puede descartar el token localmente y redirigir a login.
4. `sort` se ignora: no construir UI de ordenación sin cambiar el backend.
5. CORS no permite PUT/PATCH ni credentials: el proxy de Vite es relevante en desarrollo, pero en producción un origen distinto a `localhost:3000|5173` necesita configuración.
6. Consentimiento: no se puede "volver a dar" desde la UI sin emitir un nuevo invite.

---

## 4. Árbol de decisiones para la sesión

### Ronda 1 — frontera propuesta (prerrequisitos ya resueltos)

> Formato sugerido al retomar la sesión: `❓ Qn — título` + `➡️ respuesta recomendada`, en español, una ronda a la vez.

**Q1 — Ubicación del proyecto frontend.** ¿Dónde vive la app: directorio `frontend/` en este mismo repo, `web/`, o repo separado? ¿Se añade tooling de monorepo?
➡️ `frontend/` en este repo, Vite como herramienta, sin monorepo todavía: el backend es un único módulo y no hay señales de multi-package.

**Q2 — Conexión a la API en desarrollo.** ¿Proxy de Vite `/api` → `http://localhost:8070`, o URL absoluta configurable? (CORS ya permite 5173.)
➡️ Proxy de Vite con `baseURL='/api/v1'`; la misma base se configura por env para producción. Evita depender de CORS en local y mantiene un único punto de cambio.

**Q3 — Alcance del primer corte.** ¿Qué flujos se implementan primero? El backend ya soporta: registro con fotos, login, crear/listar/ver familias, listar miembros, emitir/canjear invites, ver DNI/fotos, revocar consentimiento y remover miembros.
➡️ Cubrir todos los flujos existentes en el primer corte (son 16 endpoints y el contrato está maduro), excluyendo explícitamente cualquier operación de edición/borrado/ordenación inexistente.

**Q4 — Fuente de verdad de los tipos.** El OpenAPI solo existe en runtime. ¿Snapshot de `/v3/api-docs` + generación de tipos, o tipos escritos a mano desde los 9 records?
➡️ Snapshot versionado en `frontend/api/openapi.json` con un script de refresco y generación de tipos (p. ej. `openapi-typescript`); el backend sigue siendo la fuente, y un diff del spec detecta deriva. Decisiones ADR-worthy.

**Q5 — Cliente HTTP y estado del servidor.** ¿Wrapper tipado sobre `fetch` + TanStack Query, o hooks con `fetch` a secas? ¿Se necesita estado global (Redux/Zustand)?
➡️ Wrapper tipado sobre `fetch` + TanStack Query para caché, 401 global y reintentos; sin librería de estado global. Aplicar `priority: 'low'` solo a prefetch.

**Q6 — Persistencia del token.** Token de 7 días sin refresh. ¿Memoria, `localStorage`, `sessionStorage`? ¿Cómo se maneja el 401?
➡️ Memoria en el primer corte + redirect a `/login` en 401; diferir "recordarme"/persistencia a una decisión explícita posterior. Trade-off de XSS vs. recarga: documentar.

**Q7 — Enrutado.** ¿Estructura de rutas y protección?
➡️ React Router: `/login`, `/register`, `/families`, `/families/:familyId`, `/invites/:code`; rutas protegidas por presencia de token, sin guardas de rol en cliente (la autorización real está en el servidor).

**Q8 — Huella documental de la sesión.** ¿Qué ADRs se escriben y qué se hace con ADR-0002?
➡️ Marcar ADR-0002 como *superseded* por la realidad del bearer token; crear ADRs para (a) contrato OpenAPI versionado como fuente de tipos y (b) estrategia de sesión/token. No tocar `GLOSSARY.md` salvo que aparezca un término nuevo del dominio.

### Rondas posteriores (dependen de las anteriores)

- **Ronda 2 — formularios y errores** (depende de Q3/Q4/Q5): mapeo de `code` → mensajes; `errors[{field,message}]` con nombres calificados; validación después de la interacción; `aria-invalid`/`:user-invalid`, live region único y foco al primer inválido; autocompletado en registro.
- **Ronda 2 — consentimiento** (depende de Q3): estados para `dni=null`, `403`, `404` y revocación self-only; explicar que re-grantar exige nuevo invite.
- **Ronda 2 — fotos** (depende de Q3): validación cliente (MIME/tamaño), preview local, mensajes para 415/413/409, uso de URLs relativas y `no-store`.
- **Ronda 2 — invitaciones** (depende de Q3): presentación y copia del código de 6 caracteres, expiración de 7 días, ausencia de historial/listado.
- **Ronda 3 — testing** (depende de Q4/Q5): contratos con `ApiIntegrationTest` como referencia, MSW vs. mocks, `migrate-to-shoehorn` para fixtures parciales en tests.
- **Ronda 3 — build y despliegue** (depende de Q1/Q2): dev serverseparado vs. copiar el build a `static/`; origen permitido por CORS en producción.
- **Ronda 3 — accesibilidad/responsive** (depende de Q3): tablas de miembros, navegación, escala de texto del SO.

---

## 5. Riesgos y deuda a visibilizar en la entrevista

- Deriva documental: ADR-0002 y spec #6 describen un backend que ya no existe.
- Sin spec versionada → el contrato puede cambiar sin que el frontend se entere.
- Sin logout/refresh/expiry en el response: la UX de sesión es una decisión de producto, no técnica.
- `Family.name` nullable en legacy: la UI debe tolerarlo.
- `sort` ignorado y `size` silenciosamente limitado a 100: no prometer ordenación ni páginas grandes.
- Estado `REMOVED` no observable desde la API.
