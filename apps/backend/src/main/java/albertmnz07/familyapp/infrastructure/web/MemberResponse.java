package albertmnz07.familyapp.infrastructure.web;

import albertmnz07.familyapp.application.query.MemberView;
import java.util.UUID;

public record MemberResponse(
        UUID personaId,
        String username,
        String dni,
        boolean admin,
        String status,
        boolean consentInForce) {

    public static MemberResponse of(MemberView member) {
        return new MemberResponse(
                member.personaId(),
                member.username(),
                member.dni(),
                member.admin(),
                member.status(),
                member.consentInForce());
    }
}
