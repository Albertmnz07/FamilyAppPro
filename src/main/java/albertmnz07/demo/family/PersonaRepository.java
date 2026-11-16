package albertmnz07.demo.family;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PersonaRepository extends JpaRepository<Persona, UUID> {

    boolean existsByDni(String dni);

    boolean existsByUsername(String username);
}
