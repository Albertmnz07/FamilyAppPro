package albertmnz07.familyapp.domain.valueobject;

import albertmnz07.familyapp.domain.exception.InvalidValueException;
import java.util.Objects;
import java.util.UUID;

public record PersonaId(UUID value) {

    public PersonaId {
        Objects.requireNonNull(value, "value");
    }

    public static PersonaId generate() {
        return new PersonaId(UUID.randomUUID());
    }

    public static PersonaId parse(String raw) {
        try {
            return new PersonaId(UUID.fromString(raw));
        } catch (IllegalArgumentException | NullPointerException invalid) {
            throw new InvalidValueException("Invalid persona id: " + raw);
        }
    }
}
