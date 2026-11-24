package albertmnz07.familyapp.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record MembershipId(UUID value) {

    public MembershipId {
        Objects.requireNonNull(value, "value");
    }

    public static MembershipId generate() {
        return new MembershipId(UUID.randomUUID());
    }
}
