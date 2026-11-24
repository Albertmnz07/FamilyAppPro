package albertmnz07.familyapp.application.query;

import albertmnz07.familyapp.domain.model.Membership;
import albertmnz07.familyapp.domain.repository.FamilyRepository;
import albertmnz07.familyapp.domain.repository.MembershipRepository;
import albertmnz07.familyapp.domain.valueobject.FamilyId;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import java.util.Comparator;
import java.util.List;

public class ListMyFamiliesQueryHandler
        implements QueryHandler<ListMyFamiliesQuery, PagedResult<FamilyView>> {

    private final MembershipRepository memberships;
    private final FamilyRepository families;

    public ListMyFamiliesQueryHandler(MembershipRepository memberships, FamilyRepository families) {
        this.memberships = memberships;
        this.families = families;
    }

    @Override
    public PagedResult<FamilyView> handle(ListMyFamiliesQuery query) {
        PersonaId personaId = new PersonaId(query.personaId());
        List<FamilyId> familyIds =
                memberships.findAcceptedByPersona(personaId).stream()
                        .map(Membership::familyId)
                        .toList();
        List<FamilyView> sorted =
                familyIds.isEmpty()
                        ? List.of()
                        : families.findAllByIds(familyIds).stream()
                                .sorted(
                                        Comparator.comparing(
                                                family -> family.name().value(),
                                                String.CASE_INSENSITIVE_ORDER))
                                .map(FamilyView::of)
                                .toList();
        return PagedResult.paginate(sorted, query.pageQuery());
    }
}
