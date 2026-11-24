package albertmnz07.familyapp.application.command;

import java.util.UUID;

public record IssueInviteCommand(UUID actorPersonaId, UUID familyId) {}
