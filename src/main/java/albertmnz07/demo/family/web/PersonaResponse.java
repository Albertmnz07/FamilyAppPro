package albertmnz07.demo.family.web;

import albertmnz07.demo.family.Persona;
import java.util.UUID;

public record PersonaResponse(
        UUID id,
        String username,
        String dni,
        String frontPhotoUrl,
        String backPhotoUrl) {

    public static PersonaResponse of(Persona persona) {
        return new PersonaResponse(
                persona.getId(),
                persona.getUsername(),
                persona.getDni(),
                "/api/v1/personas/me/photos/front",
                "/api/v1/personas/me/photos/back");
    }
}
