package albertmnz07.familyapp.legacy.family.web;

import albertmnz07.familyapp.legacy.family.Family;
import java.util.UUID;

public record FamilyResponse(UUID id, String name, UUID adminPersonaId) {

    public static FamilyResponse of(Family family) {
        return new FamilyResponse(family.getId(), family.getName(), family.getAdmin().getId());
    }
}
