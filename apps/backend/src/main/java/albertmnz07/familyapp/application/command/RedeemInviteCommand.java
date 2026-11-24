package albertmnz07.familyapp.application.command;

import java.util.UUID;

public record RedeemInviteCommand(UUID personaId, String code) {}
