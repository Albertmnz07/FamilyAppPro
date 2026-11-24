package albertmnz07.familyapp.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record PersonaId(UUID value) {

    public PersonaId {
        Objects.requireNonNull(value, "value");
    }

    public static PersonaId generate() {
        return new PersonaId(UUID.randomUUID());
    }
}
