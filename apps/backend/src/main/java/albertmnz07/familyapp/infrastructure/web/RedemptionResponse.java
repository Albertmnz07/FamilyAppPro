package albertmnz07.familyapp.infrastructure.web;

import albertmnz07.familyapp.application.command.RedeemInviteResult;
import java.util.UUID;

public record RedemptionResponse(UUID familyId, UUID personaId, String status) {

    public static RedemptionResponse of(RedeemInviteResult result) {
        return new RedemptionResponse(result.familyId(), result.personaId(), result.status());
    }
}
