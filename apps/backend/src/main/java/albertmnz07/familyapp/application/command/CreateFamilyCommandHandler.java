package albertmnz07.familyapp.application.command;

import albertmnz07.familyapp.application.port.Clock;
import albertmnz07.familyapp.application.query.FamilyView;
import albertmnz07.familyapp.domain.model.Family;
import albertmnz07.familyapp.domain.model.Membership;
import albertmnz07.familyapp.domain.repository.FamilyRepository;
import albertmnz07.familyapp.domain.repository.MembershipRepository;
import albertmnz07.familyapp.domain.valueobject.FamilyName;
import albertmnz07.familyapp.domain.valueobject.PersonaId;

public class CreateFamilyCommandHandler implements CommandHandler<CreateFamilyCommand, FamilyView> {

    private final FamilyRepository families;
    private final MembershipRepository memberships;
    private final Clock clock;

    public CreateFamilyCommandHandler(
            FamilyRepository families, MembershipRepository memberships, Clock clock) {
        this.families = families;
        this.memberships = memberships;
        this.clock = clock;
    }

    @Override
    public FamilyView handle(CreateFamilyCommand command) {
        PersonaId creatorId = new PersonaId(command.creatorPersonaId());
        FamilyName name = FamilyName.of(command.name());
        Family family = families.save(Family.create(name, creatorId));
        memberships.save(Membership.acceptNew(family.id(), creatorId, clock.now()));
        return FamilyView.of(family);
    }
}
