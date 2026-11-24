package albertmnz07.familyapp.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentJpaRepository extends JpaRepository<ConsentJpaEntity, UUID> {

    Optional<ConsentJpaEntity> findByFamilyIdAndPersonaId(UUID familyId, UUID personaId);

    List<ConsentJpaEntity> findByPersonaId(UUID personaId);

    List<ConsentJpaEntity> findByFamilyId(UUID familyId);
}
