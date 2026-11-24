package albertmnz07.familyapp.application.query;

import albertmnz07.familyapp.domain.exception.ForbiddenException;
import albertmnz07.familyapp.domain.exception.ResourceNotFoundException;
import albertmnz07.familyapp.domain.model.Family;
import albertmnz07.familyapp.domain.repository.FamilyRepository;
import albertmnz07.familyapp.domain.repository.MembershipRepository;
import albertmnz07.familyapp.domain.valueobject.FamilyId;
import albertmnz07.familyapp.domain.valueobject.PersonaId;

final class FamilyAccess {

    private FamilyAccess() {}

    static Family requireMemberAccess(
            FamilyRepository families,
            MembershipRepository memberships,
            FamilyId familyId,
            PersonaId viewerId) {
        Family family =
                families.findById(familyId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Family not found: " + familyId.value()));
        if (!memberships.existsAccepted(family.id(), viewerId)) {
            throw new ForbiddenException("Persona is not a member of this family");
        }
        return family;
    }
}
