package albertmnz07.familyapp.infrastructure.web;

import java.util.UUID;

public record LoginResponse(String token, UUID personaId, String username) {}
