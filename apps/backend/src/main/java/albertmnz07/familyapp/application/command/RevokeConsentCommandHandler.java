package albertmnz07.familyapp.application.command;

import albertmnz07.familyapp.application.port.Clock;
import albertmnz07.familyapp.domain.exception.ForbiddenException;
import albertmnz07.familyapp.domain.exception.NotFoundException;
import albertmnz07.familyapp.domain.model.Family;
import albertmnz07.familyapp.domain.model.Membership;
import albertmnz07.familyapp.domain.repository.FamilyRepository;
import albertmnz07.familyapp.domain.repository.MembershipRepository;
import albertmnz07.familyapp.domain.valueobject.FamilyId;
import albertmnz07.familyapp.domain.valueobject.PersonaId;

public class RevokeConsentCommandHandler implements CommandHandler<RevokeConsentCommand, Void> {

    private final FamilyRepository families;
    private final MembershipRepository memberships;
    private final Clock clock;

    public RevokeConsentCommandHandler(
            FamilyRepository families, MembershipRepository memberships, Clock clock) {
        this.families = families;
        this.memberships = memberships;
        this.clock = clock;
    }

    @Override
    public Void handle(RevokeConsentCommand command) {
        PersonaId actorId = new PersonaId(command.actorPersonaId());
        PersonaId targetId = new PersonaId(command.targetPersonaId());
        if (!actorId.equals(targetId)) {
            throw new ForbiddenException("A persona can only revoke its own consent");
        }
        FamilyId familyId = new FamilyId(command.familyId());
        Family family =
                families.findById(familyId)
                        .orElseThrow(() ->
                                new NotFoundException("Family not found: " + familyId.value()));
        memberships
                .findByFamilyAndPersona(family.id(), actorId)
                .filter(Membership::isConsentInForce)
                .ifPresent(membership -> {
                    membership.revokeConsent(clock.now());
                    memberships.save(membership);
                });
        return null;
    }
}
