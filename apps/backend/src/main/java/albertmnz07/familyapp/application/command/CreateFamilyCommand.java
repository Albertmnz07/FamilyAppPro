package albertmnz07.familyapp.application.command;

import java.util.UUID;

public record CreateFamilyCommand(UUID creatorPersonaId, String name) {}
