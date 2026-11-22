package albertmnz07.demo.family.web;

import albertmnz07.demo.family.Family;
import java.util.UUID;

public record FamilyResponse(UUID id, String name, UUID adminPersonaId) {

    public static FamilyResponse of(Family family) {
        return new FamilyResponse(family.getId(), family.getName(), family.getAdmin().getId());
    }
}
