package albertmnz07.familyapp.infrastructure.web;

import albertmnz07.familyapp.application.command.IssueInviteResult;
import java.time.Instant;
import java.util.UUID;

public record InviteResponse(String code, UUID familyId, Instant expiresAt) {

    public static InviteResponse of(IssueInviteResult invite) {
        return new InviteResponse(invite.code(), invite.familyId(), invite.expiresAt());
    }
}
