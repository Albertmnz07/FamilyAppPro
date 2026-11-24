package albertmnz07.familyapp.application.query;

import albertmnz07.familyapp.domain.model.Family;
import albertmnz07.familyapp.domain.repository.FamilyRepository;
import albertmnz07.familyapp.domain.repository.MembershipRepository;
import albertmnz07.familyapp.domain.valueobject.FamilyId;
import albertmnz07.familyapp.domain.valueobject.PersonaId;

public class GetFamilyQueryHandler implements QueryHandler<GetFamilyQuery, FamilyView> {

    private final FamilyRepository families;
    private final MembershipRepository memberships;

    public GetFamilyQueryHandler(FamilyRepository families, MembershipRepository memberships) {
        this.families = families;
        this.memberships = memberships;
    }

    @Override
    public FamilyView handle(GetFamilyQuery query) {
        Family family =
                FamilyAccess.requireMemberAccess(
                        families,
                        memberships,
                        new FamilyId(query.familyId()),
                        new PersonaId(query.viewerPersonaId()));
        return FamilyView.of(family);
    }
}
