package albertmnz07.familyapp.domain.valueobject;

import albertmnz07.familyapp.domain.exception.InvalidValueException;
import java.util.Objects;
import java.util.UUID;

public record MembershipId(UUID value) {

    public MembershipId {
        Objects.requireNonNull(value, "value");
    }

    public static MembershipId generate() {
        return new MembershipId(UUID.randomUUID());
    }

    public static MembershipId parse(String raw) {
        try {
            return new MembershipId(UUID.fromString(raw));
        } catch (IllegalArgumentException | NullPointerException invalid) {
            throw new InvalidValueException("Invalid membership id: " + raw);
        }
    }
}
