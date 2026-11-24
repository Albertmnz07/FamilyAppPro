package albertmnz07.familyapp.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record FamilyId(UUID value) {

    public FamilyId {
        Objects.requireNonNull(value, "value");
    }

    public static FamilyId generate() {
        return new FamilyId(UUID.randomUUID());
    }
}
