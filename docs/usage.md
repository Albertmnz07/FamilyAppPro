# Guía de uso

Instrucciones para arrancar FamilyAppPro con Docker y recorrer el circuito completo (registro, familia, invitación, consentimiento y fotos del DNI). Es un complemento práctico del README; el detalle de decisiones está en [ADR-0008](adr/0008-containerized-deployment.md) y [ADR-0006](adr/0006-frontend-location-and-api-base-url.md).

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

Con las herramientas locales instaladas, las suites completas se ejecutan con `./mvnw test` y `npm --prefix frontend run check`.

**Limitación conocida:** no hay SPA fallback. Entra en `http://localhost:8070` y navega desde la app; recargar directamente una ruta como `/families/{id}` devuelve 404.
