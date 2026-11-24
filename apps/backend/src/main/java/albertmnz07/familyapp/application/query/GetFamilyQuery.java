package albertmnz07.familyapp.application.query;

import java.util.UUID;

public record GetFamilyQuery(UUID viewerPersonaId, UUID familyId) {}
