package albertmnz07.familyapp.application.query;

import java.util.UUID;

public record MemberDniView(
        UUID familyId,
        UUID personaId,
        String number,
        boolean hasFrontPhoto,
        boolean hasBackPhoto) {}
