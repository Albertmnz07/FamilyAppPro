package albertmnz07.demo.family.web;

import albertmnz07.demo.family.Membership;
import java.util.UUID;

public record RedemptionResponse(
        UUID familyId,
        UUID personaId,
        String status) {

    public static RedemptionResponse of(Membership membership) {
        return new RedemptionResponse(
                membership.getFamily().getId(),
                membership.getPersona().getId(),
                membership.getStatus().name());
    }
}
