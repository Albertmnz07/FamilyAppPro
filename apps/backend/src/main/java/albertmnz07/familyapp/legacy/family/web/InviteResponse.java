package albertmnz07.familyapp.legacy.family.web;

import albertmnz07.familyapp.legacy.family.Invite;
import java.time.Instant;
import java.util.UUID;

public record InviteResponse(String code, UUID familyId, Instant expiresAt) {

    public static InviteResponse of(Invite invite) {
        return new InviteResponse(
                invite.getCode(), invite.getFamily().getId(), invite.getExpiresAt());
    }
}
