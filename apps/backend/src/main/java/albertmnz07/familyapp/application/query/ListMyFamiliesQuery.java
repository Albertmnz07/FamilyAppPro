package albertmnz07.familyapp.application.query;

import java.util.UUID;

public record ListMyFamiliesQuery(UUID personaId, PageQuery pageQuery) {}
