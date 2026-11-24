package albertmnz07.familyapp.application.query;

import albertmnz07.familyapp.domain.exception.ResourceNotFoundException;
import albertmnz07.familyapp.domain.model.Family;
import albertmnz07.familyapp.domain.model.Membership;
import albertmnz07.familyapp.domain.model.Persona;
import albertmnz07.familyapp.domain.repository.FamilyRepository;
import albertmnz07.familyapp.domain.repository.MembershipRepository;
import albertmnz07.familyapp.domain.repository.PersonaRepository;
import albertmnz07.familyapp.domain.valueobject.Dni;
import albertmnz07.familyapp.domain.valueobject.FamilyId;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ListFamilyMembersQueryHandler
        implements QueryHandler<ListFamilyMembersQuery, PagedResult<MemberView>> {

    private final FamilyRepository families;
    private final MembershipRepository memberships;
    private final PersonaRepository personas;

    public ListFamilyMembersQueryHandler(
            FamilyRepository families,
            MembershipRepository memberships,
            PersonaRepository personas) {
        this.families = families;
        this.memberships = memberships;
        this.personas = personas;
    }

    @Override
    public PagedResult<MemberView> handle(ListFamilyMembersQuery query) {
        Family family =
                FamilyAccess.requireMemberAccess(
                        families,
                        memberships,
                        new FamilyId(query.familyId()),
                        new PersonaId(query.viewerPersonaId()));
        List<Membership> accepted = memberships.findAcceptedByFamily(family.id());
        if (accepted.isEmpty()) {
            return PagedResult.paginate(List.of(), query.pageQuery());
        }
        Map<PersonaId, Persona> personasById =
                personas
                        .findAllByIds(accepted.stream().map(Membership::personaId).toList())
                        .stream()
                        .collect(Collectors.toMap(Persona::id, Function.identity()));
        List<MemberView> sorted =
                accepted.stream()
                        .sorted(
                                Comparator.comparing(
                                        membership ->
                                                requirePersona(personasById, membership.personaId())
                                                        .dniValue(),
                                        String.CASE_INSENSITIVE_ORDER))
                        .map(membership -> {
                            Persona persona = requirePersona(personasById, membership.personaId());
                            String visibleDni =
                                    membership
                                            .visibleDni(persona.dni())
                                            .map(Dni::value)
                                            .orElse(null);
                            return new MemberView(
                                    persona.id().value(),
                                    persona.username().value(),
                                    visibleDni,
                                    family.isAdmin(persona.id()),
                                    membership.status().name(),
                                    membership.isConsentInForce());
                        })
                        .toList();
        return PagedResult.paginate(sorted, query.pageQuery());
    }

    private static Persona requirePersona(Map<PersonaId, Persona> personasById, PersonaId id) {
        Persona persona = personasById.get(id);
        if (persona == null) {
            throw new ResourceNotFoundException("Persona not found: " + id.value());
        }
        return persona;
    }
}
