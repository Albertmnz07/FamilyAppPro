package albertmnz07.familyapp.domain.repository;

import albertmnz07.familyapp.domain.model.Membership;
import albertmnz07.familyapp.domain.valueobject.FamilyId;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import java.util.List;
import java.util.Optional;

public interface MembershipRepository {

    Membership save(Membership membership);

    Optional<Membership> findByFamilyAndPersona(FamilyId familyId, PersonaId personaId);

    boolean existsAccepted(FamilyId familyId, PersonaId personaId);

    List<Membership> findAcceptedByPersona(PersonaId personaId);

    List<Membership> findAcceptedByFamily(FamilyId familyId);
}
