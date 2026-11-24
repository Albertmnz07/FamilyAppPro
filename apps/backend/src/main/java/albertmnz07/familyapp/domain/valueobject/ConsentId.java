package albertmnz07.familyapp.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record ConsentId(UUID value) {

    public ConsentId {
        Objects.requireNonNull(value, "value");
    }

    public static ConsentId generate() {
        return new ConsentId(UUID.randomUUID());
    }
}
