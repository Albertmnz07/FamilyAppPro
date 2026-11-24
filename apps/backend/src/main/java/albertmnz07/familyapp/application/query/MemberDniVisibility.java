package albertmnz07.familyapp.application.query;

import albertmnz07.familyapp.domain.exception.ForbiddenException;
import albertmnz07.familyapp.domain.exception.NotFoundException;
import albertmnz07.familyapp.domain.model.Family;
import albertmnz07.familyapp.domain.model.Membership;
import albertmnz07.familyapp.domain.model.Persona;
import albertmnz07.familyapp.domain.repository.FamilyRepository;
import albertmnz07.familyapp.domain.repository.MembershipRepository;
import albertmnz07.familyapp.domain.repository.PersonaRepository;
import albertmnz07.familyapp.domain.valueobject.Dni;
import albertmnz07.familyapp.domain.valueobject.FamilyId;
import albertmnz07.familyapp.domain.valueobject.PersonaId;

final class MemberDniVisibility {

    private MemberDniVisibility() {}

    static Dni requireVisibleDni(
            FamilyRepository families,
            MembershipRepository memberships,
            PersonaRepository personas,
            FamilyId familyId,
            PersonaId viewerId,
            PersonaId targetId) {
        Family family =
                families.findById(familyId)
                        .orElseThrow(() ->
                                new NotFoundException("Family not found: " + familyId.value()));
        if (!memberships.existsAccepted(family.id(), viewerId)) {
            throw new ForbiddenException("Persona is not a member of this family");
        }
        Persona target =
                personas.findById(targetId)
                        .orElseThrow(() ->
                                new NotFoundException("Persona not found: " + targetId.value()));
        Membership targetMembership =
                memberships
                        .findByFamilyAndPersona(family.id(), target.id())
                        .filter(Membership::isAccepted)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Member not found in family: " + targetId.value()));
        return targetMembership
                .visibleDni(target.dni())
                .orElseThrow(
                        () -> new ForbiddenException("Consent is not in force for this persona"));
    }
}
