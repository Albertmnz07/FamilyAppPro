package albertmnz07.familyapp.application.command;

import java.util.UUID;

public record RemoveMemberCommand(UUID actorPersonaId, UUID familyId, UUID targetPersonaId) {}
