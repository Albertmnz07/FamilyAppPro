package albertmnz07.familyapp.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonaJpaRepository extends JpaRepository<PersonaJpaEntity, UUID> {

    boolean existsByDni(String dni);

    boolean existsByUsername(String username);

    Optional<PersonaJpaEntity> findByUsername(String username);
}
