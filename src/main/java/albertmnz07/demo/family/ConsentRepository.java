package albertmnz07.demo.family;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ConsentRepository extends JpaRepository<Consent, UUID> {

    Optional<Consent> findByFamilyAndPersona(Family family, Persona persona);
}
