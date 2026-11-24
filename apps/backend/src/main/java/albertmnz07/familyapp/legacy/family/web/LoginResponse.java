package albertmnz07.familyapp.legacy.family.web;

import java.util.UUID;

public record LoginResponse(String token, UUID personaId, String username) {}
