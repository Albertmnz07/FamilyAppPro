package albertmnz07.demo.family.web;

import albertmnz07.demo.family.Membership;
import java.util.UUID;

public record MemberResponse(
        UUID personaId,
        String username,
        String dni,
        boolean admin,
        String status,
        boolean consentInForce) {

    public static MemberResponse of(Membership membership, boolean consentInForce) {
        return new MemberResponse(
                membership.getPersona().getId(),
                membership.getPersona().getUsername(),
                consentInForce ? membership.getPersona().getDni() : null,
                membership.getFamily().getAdmin().getId().equals(membership.getPersona().getId()),
                membership.getStatus().name(),
                consentInForce);
    }
}
