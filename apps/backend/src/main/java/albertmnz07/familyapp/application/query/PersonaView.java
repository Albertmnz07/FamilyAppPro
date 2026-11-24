package albertmnz07.familyapp.application.query;

import albertmnz07.familyapp.domain.model.Persona;
import java.util.UUID;

/** Read model of a persona as seen by its owner. */
public record PersonaView(
        UUID id, String username, String dni, boolean hasFrontPhoto, boolean hasBackPhoto) {

    public static PersonaView of(Persona persona) {
        return new PersonaView(
                persona.id().value(),
                persona.username().value(),
                persona.dniValue(),
                persona.dni().front().isPresent(),
                persona.dni().back().isPresent());
    }
}
