package albertmnz07.familyapp.application.query;

import java.util.UUID;

public record MemberView(
        UUID personaId,
        String username,
        String dni,
        boolean admin,
        String status,
        boolean consentInForce) {}
