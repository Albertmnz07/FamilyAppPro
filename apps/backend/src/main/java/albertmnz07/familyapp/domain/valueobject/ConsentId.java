package albertmnz07.familyapp.domain.valueobject;

import albertmnz07.familyapp.domain.exception.InvalidValueException;
import java.util.Objects;
import java.util.UUID;

public record ConsentId(UUID value) {

    public ConsentId {
        Objects.requireNonNull(value, "value");
    }

    public static ConsentId generate() {
        return new ConsentId(UUID.randomUUID());
    }

    public static ConsentId parse(String raw) {
        try {
            return new ConsentId(UUID.fromString(raw));
        } catch (IllegalArgumentException | NullPointerException invalid) {
            throw new InvalidValueException("Invalid consent id: " + raw);
        }
    }
}
