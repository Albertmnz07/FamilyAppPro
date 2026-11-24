package albertmnz07.familyapp.application.query;

import java.util.UUID;

public record GetMemberDniQuery(UUID viewerPersonaId, UUID familyId, UUID targetPersonaId) {}
