package albertmnz07.familyapp.application.query;

import albertmnz07.familyapp.domain.repository.FamilyRepository;
import albertmnz07.familyapp.domain.repository.MembershipRepository;
import albertmnz07.familyapp.domain.repository.PersonaRepository;
import albertmnz07.familyapp.domain.valueobject.Dni;
import albertmnz07.familyapp.domain.valueobject.FamilyId;
import albertmnz07.familyapp.domain.valueobject.PersonaId;

public class GetMemberDniQueryHandler implements QueryHandler<GetMemberDniQuery, MemberDniView> {

    private final FamilyRepository families;
    private final MembershipRepository memberships;
    private final PersonaRepository personas;

    public GetMemberDniQueryHandler(
            FamilyRepository families,
            MembershipRepository memberships,
            PersonaRepository personas) {
        this.families = families;
        this.memberships = memberships;
        this.personas = personas;
    }

    @Override
    public MemberDniView handle(GetMemberDniQuery query) {
        FamilyId familyId = new FamilyId(query.familyId());
        PersonaId viewerId = new PersonaId(query.viewerPersonaId());
        PersonaId targetId = new PersonaId(query.targetPersonaId());
        Dni visibleDni =
                MemberDniVisibility.requireVisibleDni(
                        families, memberships, personas, familyId, viewerId, targetId);
        return new MemberDniView(
                familyId.value(),
                targetId.value(),
                visibleDni.value(),
                visibleDni.front().isPresent(),
                visibleDni.back().isPresent());
    }
}
