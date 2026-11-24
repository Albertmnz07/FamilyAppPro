package albertmnz07.familyapp.legacy.family.web;

import albertmnz07.familyapp.legacy.family.Membership;
import java.util.UUID;

public record RedemptionResponse(UUID familyId, UUID personaId, String status) {

    public static RedemptionResponse of(Membership membership) {
        return new RedemptionResponse(
                membership.getFamily().getId(),
                membership.getPersona().getId(),
                membership.getStatus().name());
    }
}
