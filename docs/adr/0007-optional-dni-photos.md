# Optional DNI photos

The DNI number remains the mandatory, unique identifier of a Persona, but the front/back photos are optional evidence that can be uploaded later from the personal menu; registration collects only Username, DNI number and password. We accept a less complete identity at sign-up in exchange for less friction and no camera requirement at sign-up, and we raise the photo limit to 10 MB so original camera photos can be sent uncompressed.

Consequences: photo keys and content types are nullable, photo URLs in responses are `null` until upload, and a viewed DNI without photos shows the number plus "Sin fotos del DNI" instead of broken images. ADR-0001 still governs identity; this narrows only the evidence attached to it.
