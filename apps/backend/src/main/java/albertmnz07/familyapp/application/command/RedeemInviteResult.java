package albertmnz07.familyapp.application.command;

import java.util.UUID;

public record RedeemInviteResult(UUID familyId, UUID personaId, String status) {}
