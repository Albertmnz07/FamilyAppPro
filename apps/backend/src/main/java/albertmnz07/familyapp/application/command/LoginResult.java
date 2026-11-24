package albertmnz07.familyapp.application.command;

import java.util.UUID;

public record LoginResult(String token, UUID personaId, String username) {}
