package albertmnz07.familyapp.application.query;

import java.util.UUID;

public record ListFamilyMembersQuery(UUID viewerPersonaId, UUID familyId, PageQuery pageQuery) {}
