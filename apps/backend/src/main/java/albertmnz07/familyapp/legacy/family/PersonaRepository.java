package albertmnz07.familyapp.legacy.family;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonaRepository extends JpaRepository<Persona, UUID> {

    boolean existsByDni(String dni);

    boolean existsByUsername(String username);

    Optional<Persona> findByUsername(String username);
}
