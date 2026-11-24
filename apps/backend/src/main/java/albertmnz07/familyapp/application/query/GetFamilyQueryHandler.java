package albertmnz07.familyapp.application.query;

import albertmnz07.familyapp.domain.exception.ForbiddenException;
import albertmnz07.familyapp.domain.exception.NotFoundException;
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
        FamilyId familyId = new FamilyId(query.familyId());
        PersonaId viewerId = new PersonaId(query.viewerPersonaId());
        Family family =
                families.findById(familyId)
                        .orElseThrow(() ->
                                new NotFoundException("Family not found: " + familyId.value()));
        if (!memberships.existsAccepted(family.id(), viewerId)) {
            throw new ForbiddenException("Persona is not a member of this family");
        }
        return FamilyView.of(family);
    }
}
