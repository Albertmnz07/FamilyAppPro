package albertmnz07.familyapp.infrastructure.web;

import albertmnz07.familyapp.application.query.PersonaView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record PersonaResponse(
        UUID id,
        String username,
        String dni,
        @Schema(nullable = true) String frontPhotoUrl,
        @Schema(nullable = true) String backPhotoUrl) {

    public static PersonaResponse of(PersonaView persona) {
        return new PersonaResponse(
                persona.id(),
                persona.username(),
                persona.dni(),
                persona.hasFrontPhoto() ? "/api/v1/personas/me/photos/front" : null,
                persona.hasBackPhoto() ? "/api/v1/personas/me/photos/back" : null);
    }
}
