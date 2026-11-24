package albertmnz07.familyapp.application.command;

import albertmnz07.familyapp.application.port.Clock;
import albertmnz07.familyapp.domain.exception.ResourceNotFoundException;
import albertmnz07.familyapp.domain.model.Family;
import albertmnz07.familyapp.domain.model.Membership;
import albertmnz07.familyapp.domain.repository.FamilyRepository;
import albertmnz07.familyapp.domain.repository.MembershipRepository;
import albertmnz07.familyapp.domain.valueobject.FamilyId;
import albertmnz07.familyapp.domain.valueobject.PersonaId;

public class RemoveMemberCommandHandler implements CommandHandler<RemoveMemberCommand, Void> {

    private final FamilyRepository families;
    private final MembershipRepository memberships;
    private final Clock clock;

    public RemoveMemberCommandHandler(
            FamilyRepository families, MembershipRepository memberships, Clock clock) {
        this.families = families;
        this.memberships = memberships;
        this.clock = clock;
    }

    @Override
    public Void handle(RemoveMemberCommand command) {
        FamilyId familyId = new FamilyId(command.familyId());
        PersonaId actorId = new PersonaId(command.actorPersonaId());
        PersonaId targetId = new PersonaId(command.targetPersonaId());
        Family family =
                families.findById(familyId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Family not found: " + familyId.value()));
        family.requireAdmin(actorId);
        Membership membership =
                memberships
                        .findByFamilyAndPersona(family.id(), targetId)
                        .filter(Membership::isAccepted)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Member not found in family: " + targetId.value()));
        family.requireRemovable(targetId);
        membership.remove(clock.now());
        memberships.save(membership);
        return null;
    }
}
