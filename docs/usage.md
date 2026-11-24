# Guía de uso

Instrucciones para arrancar FamilyAppPro con Docker y recorrer el circuito completo (registro, familia, invitación, consentimiento y fotos del DNI). Es un complemento práctico del [README](../README.md); el código vive bajo `apps/backend` (API Spring Boot con arquitectura hexagonal) y `apps/frontend` (SPA React + TypeScript + Vite), mientras que la orquestación (`Makefile`, `Dockerfile`, `compose.*.yml`) se ejecuta desde la raíz del repositorio ([ADR-0010](adr/0010-hexagonal-backend-architecture.md), [ADR-0008](adr/0008-containerized-deployment.md) y [ADR-0006](adr/0006-frontend-location-and-api-base-url.md)).

## Requisitos

- Docker Engine con el plugin Compose y `git`. No hacen falta Java, Node ni PostgreSQL en la máquina.
- Para el despliegue en el segundo PC, que esté en la misma LAN y saber su IP (`hostname -I` en Linux, `ipconfig` en Windows).
- Opcionalmente, copia [`.env.example`](../.env.example) a `.env` para cambiar los valores por defecto. `.env` está ignorado por git.

## Desarrollo (un solo comando)

Desde la raíz del repositorio:

```bash
git clone https://github.com/Albertmnz07/FamilyAppPro.git
cd FamilyAppPro
docker compose -f compose.dev.yml up --build
```

| Servicio | Dirección | Función |
| --- | --- | --- |
| front-end | <http://localhost:5173> | app (Vite, hot reload) |
| API | <http://localhost:8070> | endpoints y Swagger UI |
| PostgreSQL | solo en la red de compose | base de datos (no publicada) |

- Swagger UI: <http://localhost:8070/swagger-ui.html>; OpenAPI: <http://localhost:8070/v3/api-docs>.
- El navegador está en `localhost:5173` y la app llama a la API en `http://localhost:8070/api/v1` (origen permitido por CORS).
- Los cambios en TypeScript/React se recargan solos. Tras cambiar Java, ejecuta `docker compose -f compose.dev.yml restart api` para recompilar; no hace falta reconstruir la imagen.
- En arranques posteriores puedes omitir `--build`: `docker compose -f compose.dev.yml up`.
- Para parar: `docker compose -f compose.dev.yml down`.

## Atajos con make

La raíz incluye un [`Makefile`](../Makefile) que reúne estos comandos. Ejecuta `make` (o `make help`) para ver la lista completa.

| Comando | Efecto |
| --- | --- |
| `make setup` | activa los git hooks de `.githooks/` en este clon (una vez) |
| `make dev` | arranca el stack de desarrollo y sigue los logs |
| `make dev-detached` | arranca el stack de desarrollo en segundo plano |
| `make dev-down` | para el stack de desarrollo (conserva los datos) |
| `make dev-restart` | reinicia la API para recompilar los cambios en `apps/backend` |
| `make dev-logs` | sigue los logs del stack de desarrollo |
| `make prod` | reconstruye y redespliega los servicios de producción modificados (`up -d --build`) |
| `make prod-start` | arranca el stack de producción sin reconstruir imágenes (`up -d --no-build`) |
| `make prod-rebuild` | fuerza una reconstrucción completa de la imagen de la API (`build --no-cache api` + `up -d`) |
| `make prod-down` | para el stack de producción (conserva los datos) |
| `make prod-logs` | sigue los logs de la API en producción |
| `make db-reset-dev` | recrea la base de datos de desarrollo desde cero |
| `make db-reset-prod` | recrea la base de datos de producción desde cero |
| `make test` | ejecuta las suites de `apps/backend` y `apps/frontend` |
| `make test-backend` | ejecuta la suite del back-end (`cd apps/backend && ./mvnw test`) |
| `make test-frontend` / `make check` | ejecuta typecheck, lint y tests del front-end (`npm --prefix apps/frontend run check`) |
| `make format` | aplica Spotless (`apps/backend`) y Prettier (`apps/frontend`) |
| `make format-check` | verifica el formato sin modificar archivos |
| `make build` | construye el JAR único en `apps/backend/target/` con `apps/frontend/dist` integrado |
| `make run` | construye el artefacto único y lo ejecuta en <http://localhost:8070> |
| `make clean` | elimina `apps/backend/target` y `apps/frontend/dist` |

Los `up` de producción recrean solo los servicios cuya imagen o configuración cambió, así no se acumulan contenedores viejos. Los resets hacen `down -v`, que **borra el volumen `pgdata`** pero deja `./data/photos` intacto.

## Segundo PC ("fake production")

```bash
git clone https://github.com/Albertmnz07/FamilyAppPro.git
cd FamilyAppPro
docker compose -f compose.prod.yml up -d --build
```

Abre <http://localhost:8070> en ese PC o `http://<IP-LAN>:8070` desde otro dispositivo de la LAN. Solo se ejecutan PostgreSQL y una imagen del back-end: el mismo jar sirve la API y la app compilada en un único origen, con `/api/v1` relativo, de modo que la imagen funciona con cualquier nombre de host sin recompilar.

Es un ejercicio de despliegue en LAN de confianza: sin TLS, registro de imágenes, proxy inverso ni credenciales nuevas. El proyecto usa `familyapp`/`familyapp` por defecto; cambia `.env` si hace falta.

Comandos útiles:

```bash
docker compose -f compose.prod.yml ps                  # estado de los servicios
docker compose -f compose.prod.yml logs -f api         # logs de la API
docker compose -f compose.prod.yml restart api         # reiniciar la API
docker compose -f compose.prod.yml down                # parar sin borrar datos
```

Ambos stacks publican el puerto 8070: para uno antes de arrancar el otro. Para ejecutar la app sin Docker, consulta la sección «Deploy it on localhost» del README.

## Variables de entorno

| Variable | Valor por defecto | Uso |
| --- | --- | --- |
| `POSTGRES_USER` | `familyapp` | usuario de PostgreSQL |
| `POSTGRES_PASSWORD` | `familyapp` | contraseña de PostgreSQL |
| `POSTGRES_DB` | `familyapp` | base de datos creada por la imagen oficial |
| `VITE_API_BASE_URL` | `http://localhost:8070/api/v1` | URL base de la API en desarrollo; producción fija `/api/v1` en el build |

## Datos y persistencia

- La base de datos vive en el volumen con nombre `pgdata`; Flyway aplica las migraciones al arrancar la API.
- Las fotos del DNI viven en `./data/photos` (montaje enlazado, `PHOTOS_DIR=/app/data/photos` en producción).
- `docker compose -f <archivo> down` conserva ambos. `down -v` **borra el volumen de la base de datos**; para borrar también las fotos hay que eliminar `data/photos` a mano.
- Los proyectos se llaman `familyapp-dev` y `familyapp-prod`, así que sus volúmenes `pgdata` son independientes.

## Primer uso

El circuito necesita dos personas. Usa un segundo perfil del navegador o una ventana privada para la segunda, para no compartir el token de sesión.

1. **Registro** en `/register`: nombre de usuario (3–30 caracteres: letras, dígitos, punto, guion bajo o guion), DNI (no vacío y único) y contraseña (3–100 caracteres). No se piden fotos al registrarse.
2. **Inicio de sesión** en `/login`. La sesión dura siete días; al caducar vuelve a la pantalla de login.
3. **Fotos del DNI (opcional)** desde el menú personal de `/mi-perfil`: `Subir fotos del DNI` acepta anverso y/o reverso desde el archivo o la cámara (JPEG/PNG, hasta 10 MB cada una). Se pueden añadir o reemplazar en cualquier momento.
4. **Crear una familia** en `/families`. Quien la crea es Admin y aparece con una insignia.
5. **Emitir una invitación** en el espacio de la familia: `Emitir invitación` muestra un código de un solo uso con caducidad de siete días.
6. **Canjear la invitación** con la segunda persona: pega el código en `Canjear una invitación` de `/families` o abre `/invites/<code>`. Al unirse se acepta el consentimiento y el DNI pasa a ser visible para la familia.
7. **Ver un DNI** con `Ver DNI` en la fila del miembro; pulsa una foto para ampliarla (cierre con la cruz, tocando fuera o con Escape). Sin fotos aparece `Sin fotos del DNI`.
8. **Revocar el consentimiento** propio con `Revocar consentimiento` en tu fila: el DNI deja de mostrarse, pero sigues siendo miembro.
9. **Retirar un miembro** (solo Admin) con `Retirar`. El Admin de la familia no puede ser retirado.

## Comprobación rápida

```bash
curl -fsS http://localhost:8070/v3/api-docs >/dev/null && echo "API OK"
docker compose -f compose.dev.yml ps
```

Con las herramientas locales instaladas, las suites completas se ejecutan con `make test` — o por separado con `(cd apps/backend && ./mvnw test)` y `npm --prefix apps/frontend run check` (`make check`). Si cambia el contrato REST de la API, con el back-end escuchando en `http://localhost:8070` actualiza la instantánea OpenAPI y los tipos generados con `npm --prefix apps/frontend run api:refresh` ([ADR-0005](adr/0005-openapi-snapshot-as-frontend-contract.md)).

**Limitación conocida:** no hay SPA fallback. Entra en `http://localhost:8070` y navega desde la app; recargar directamente una ruta como `/families/{id}` devuelve 404.
