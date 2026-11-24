package albertmnz07.familyapp.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record AuthTokenId(UUID value) {

    public AuthTokenId {
        Objects.requireNonNull(value, "value");
    }

    public static AuthTokenId generate() {
        return new AuthTokenId(UUID.randomUUID());
    }
}
