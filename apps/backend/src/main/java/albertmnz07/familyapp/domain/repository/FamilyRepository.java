package albertmnz07.familyapp.domain.repository;

import albertmnz07.familyapp.domain.model.Family;
import albertmnz07.familyapp.domain.valueobject.FamilyId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FamilyRepository {

    Family save(Family family);

    Optional<Family> findById(FamilyId id);

    List<Family> findAllByIds(Collection<FamilyId> ids);
}
