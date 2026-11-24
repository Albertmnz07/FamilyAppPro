package albertmnz07.familyapp.application.command;

import java.time.Instant;
import java.util.UUID;

public record IssueInviteResult(String code, UUID familyId, Instant expiresAt) {}
