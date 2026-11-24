package albertmnz07.familyapp.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record InviteId(UUID value) {

    public InviteId {
        Objects.requireNonNull(value, "value");
    }

    public static InviteId generate() {
        return new InviteId(UUID.randomUUID());
    }
}
