package albertmnz07.familyapp.domain.valueobject;

import albertmnz07.familyapp.domain.exception.InvalidValueException;
import java.util.Objects;
import java.util.UUID;

public record FamilyId(UUID value) {

    public FamilyId {
        Objects.requireNonNull(value, "value");
    }

    public static FamilyId generate() {
        return new FamilyId(UUID.randomUUID());
    }

    public static FamilyId parse(String raw) {
        try {
            return new FamilyId(UUID.fromString(raw));
        } catch (IllegalArgumentException | NullPointerException invalid) {
            throw new InvalidValueException("Invalid family id: " + raw);
        }
    }
}
