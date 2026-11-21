# DNI opcional, menú personal y lightbox — contexto para la siguiente sesión

> **Estado**: spec publicada, TN-20 implementado y verificado; TN-21…TN-24 siguen abiertos.
> **Uso**: material de entrada para la sesión que continúe la implementación. **No volver a preguntar** lo que ya está decidido en la sección 2.
> **Tracker**: `Albertmnz07/FamilyAppPro` vía `gh`.

---

## 1. Spec y tickets ya publicados

- **Spec #26** — `Spec: Optional DNI photos, personal menu and mobile lightbox`:
  https://github.com/Albertmnz07/FamilyAppPro/issues/26
  Contiene el Problem Statement, las 14 historias de usuario, las decisiones de implementación/testing y el Out of Scope completos. Es la fuente canonical; este archivo solo añade el estado de ejecución.
- **#27 TN-20 — Backend base**: cerrado, implementado en esta sesión.
- **#28 TN-21 — Backend PUT + CORS**: abierto, desbloqueado. Dependencias nativas: ninguna abierta.
- **#29 TN-22 — Frontend contrato/registro/mensajes**: abierto, desbloqueado. Dependencia: #27 (cerrada).
- **#30 TN-23 — Frontend menú + cámara**: abierto, bloqueado por #28 y #29.
- **#31 TN-24 — Frontend lightbox + estado sin fotos**: abierto, bloqueado por #29.

Todos son sub-issues de #26 y llevan la etiqueta `ready-for-agent`.

---

## 2. Decisiones cerradas (no renegociar)

1. El **número de DNI sigue siendo obligatorio, único, normalizado e identificador**; Username también es único. Lo opcional son solo las **fotos**.
2. El registro es `username` + DNI + `password`, sin fotos. Se eliminó la validación de formato (letra de control) en backend y frontend; `duplicate-dni` se conserva.
3. Límite de foto **10 MB por foto / 30 MB por petición**, sin compresión en el navegador; el usuario acepta el coste de almacenamiento.
4. Endpoint pendiente: `PUT /api/v1/personas/me/dni/photos`, multipart, `frontPhoto`/`backPhoto` opcionales e independientes; la parte omitida conserva la foto anterior; sin campo `dni`; devuelve 200 con `PersonaResponse`.
5. CORS debe añadir `PUT`; el transporte del frontend también.
6. `PersonaResponse` y `DniResponse` devuelven URLs `null` cuando no hay foto. Con consentimiento pero sin fotos: número + «Sin fotos del DNI», cero peticiones a `/dni/front|back`.
7. Menú personal `/mi-perfil` protegido, enlazado desde `AppShell`: datos únicos en solo lectura + subida de fotos. Sin edición de Username/DNI ni logout.
8. Cámara **solo frontend**: `getUserMedia` + `<video>` + `<canvas>`, vista previa con «Aceptar» / «Repetir», liberar el stream, selector de archivos como fallback. Requiere HTTPS o `localhost`.
9. Lightbox simple y móvil: `<dialog>` nativo con `showModal()`, cierre con cruz / fuera de la imagen / Escape, `closedby="any"` con fallback Safari, sin `role="dialog"` ni `aria-modal`.
10. Toda la UI en español; `src/api/messages.ts` mantiene los 12 códigos de error, solo cambia el texto de `photo-too-large`.
11. Documentación ya escrita: `GLOSSARY.md` separa DNI de fotos; ADR-0001 matizado; ADR-0006 actualizado para `PUT`; ADR-0007 creado.

---

## 3. Estado real del código tras TN-20

### Backend (ya implementado y pusheado)

- `PersonaController.register` acepta multipart con solo `username`, `dni`, `password`; ya no hay `@RequestPart` de fotos.
- `DniValidator.java` y `ValidDni.java` **eliminados**; no queda validación de formato del DNI.
- `PersonaService.register(username, dni, password)` crea la Persona sin fotos; `trim().toUpperCase()`, `@NotBlank` y unicidad intactos.
- `PersonaService.validatePhoto(byte[], String, String)` es `public static`, usa 10 MB y sigue permitiendo solo JPEG/PNG. Será reutilizado por TN-21.
- `spring.servlet.multipart.max-file-size=10MB`, `max-request-size=30MB`; `ApiExceptionHandler` dice «at most 10 MB».
- `V3__make_dni_photos_optional.sql` quita `NOT NULL` a los dos content-types. La columna `dni` y uk_persona_dni no se tocan; el orden de miembros por DNI sigue igual.
- `Persona.hasFrontPhoto()/hasBackPhoto()` y `Dni.hasFrontPhoto()/hasBackPhoto()` deciden si hay foto; `PersonaResponse.of` y `DniResponse.of` emiten URLs `null` en caso contrario.
- Tests: `ApiIntegrationTest` adaptado; `SchemaMigrationTest` cubre V3 (incluido el upgrade desde baseline V1); nuevo `PersonaServicePhotoTest`.
- Verificación: `./mvnw -q test` → **64 tests, 0 fallos**.

### Frontend (todavía sin tocar en esta sesión)

- `RegisterPage` sigue pidiendo DNI con patrón/letra y dos fotos obligatorias: eso es TN-22.
- `src/api/transport.ts` restringe `method` a `GET|POST|DELETE`: TN-21/TN-22 añadirán `PUT`.
- `frontend/openapi.json` y `src/api/schema.d.ts` **aún reflejan el contrato antiguo**. Hay que ejecutar `npm run api:refresh` con el backend corriendo en `http://localhost:8070/v3/api-docs` (necesita la PostgreSQL local; no inventar el snapshot a mano).
- `FamilyWorkspacePage` muestra las fotos sin acción de clic; no existe lightbox ni estado «Sin fotos del DNI».
- No existe aún `/mi-perfil` ni enlace en `AppShell`.
- `getUserMedia`, canvas y compresión: ningún uso previo; sin dependencias nuevas.

---

## 4. Primer movimiento sugerido

1. **TN-21 (#28)**: añadir el `PUT` en `PersonaController`, validación con `PersonaService.validatePhoto`, `photos.put` + actualización de la entidad (métodos `set...` o constructor completo), CORS y tests de integración (una cara, ambas, reemplazo, 413/415, sin token). Cerrar #28 con `gh issue comment` + `gh issue close`.
2. **Refrescar el contrato**: con el backend arriba, `npm run api:refresh` en `frontend/`.
3. **TN-22 (#29)**: quitar DNI-format y fotos de `RegisterPage`, ajustar `personas.ts` y `messages.test.ts`.
4. **TN-23 (#30)** y **TN-24 (#31)**: menú/cámara y lightbox/estado sin fotos. Antes de escribir UI, ejecutar `modern-web-guidance` para `dialog`, `light dismiss`, `camera capture` y `form validation`.

---

## 5. Seams y pruebas

- Un único seam de HTTP: tests de integración `@SpringBootTest` + MockMvc en backend; `fetch` stubbeado globalmente en frontend. No probar internos ni clases CSS; afirmar estados, textos en español y roles.
- Backend ya cubre: registro sin fotos, DNI sin formato válido, normalización, duplicados, migration V3, validación de 10 MB y tipo MIME.
- Pendiente de cubrir: `PUT` (parcial, reemplazo, errores), CORS `PUT`, menú/cámara (aceptar/repetir/fallback), lightbox (cruz/fondo/Escape) y «Sin fotos del DNI» con cero fetches de foto.
- `src/api/messages.test.ts` fija la lista exacta de 12 códigos; no añadir ni quitar.
- Test frágil preexistente: `frontend/src/pages/RegisterPage.test.tsx:207` falla en la suite completa pero pasa aislado/con `--sequence.shuffle`. No atribuirlo a estos cambios sin comprobarlo.

---

## 6. Skills sugeridos para la siguiente sesión

- No repetir `grilling` ni `to-spec`: la entrevista terminó y la spec #26 ya está publicada.
- Usar la skill de implementación de specs (`implement-spec` / `implement`) con el ticket correspondiente, y `tdd` + `code-review` antes de darlo por terminado.
- Usar `modern-web-guidance` antes de cualquier trabajo de UI (cámara, diálogo, cierre por fondo, validación de formulario).
- Respetar `CODING_STANDARDS.md`: el alcance del ticket es el límite; un cambio de backend en un ticket de frontend necesita su propio ticket.
