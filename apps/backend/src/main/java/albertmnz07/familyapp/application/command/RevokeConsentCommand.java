package albertmnz07.familyapp.application.command;

import java.util.UUID;

public record RevokeConsentCommand(UUID actorPersonaId, UUID familyId, UUID targetPersonaId) {}
