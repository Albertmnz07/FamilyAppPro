package albertmnz07.familyapp.infrastructure.web;

import albertmnz07.familyapp.application.query.FamilyView;
import java.util.UUID;

public record FamilyResponse(UUID id, String name, UUID adminPersonaId) {

    public static FamilyResponse of(FamilyView family) {
        return new FamilyResponse(family.id(), family.name(), family.adminPersonaId());
    }
}
