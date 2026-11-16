package albertmnz07.demo.family.web;

import java.util.UUID;

public record LoginResponse(
        String token,
        UUID personaId,
        String username) {
}
