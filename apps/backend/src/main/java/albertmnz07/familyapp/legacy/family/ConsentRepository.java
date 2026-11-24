package albertmnz07.familyapp.legacy.family;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRepository extends JpaRepository<Consent, UUID> {

    Optional<Consent> findByFamilyAndPersona(Family family, Persona persona);
}
