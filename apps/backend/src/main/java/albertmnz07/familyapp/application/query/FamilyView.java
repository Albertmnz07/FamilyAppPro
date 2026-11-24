package albertmnz07.familyapp.application.query;

import albertmnz07.familyapp.domain.model.Family;
import java.util.UUID;

public record FamilyView(UUID id, String name, UUID adminPersonaId) {

    public static FamilyView of(Family family) {
        return new FamilyView(family.id().value(), family.name().value(), family.adminId().value());
    }
}
