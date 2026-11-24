package albertmnz07.familyapp.domain.valueobject;

import albertmnz07.familyapp.domain.exception.InvalidValueException;
import java.util.Objects;
import java.util.UUID;

public record InviteId(UUID value) {

    public InviteId {
        Objects.requireNonNull(value, "value");
    }

    public static InviteId generate() {
        return new InviteId(UUID.randomUUID());
    }

    public static InviteId parse(String raw) {
        try {
            return new InviteId(UUID.fromString(raw));
        } catch (IllegalArgumentException | NullPointerException invalid) {
            throw new InvalidValueException("Invalid invite id: " + raw);
        }
    }
}
