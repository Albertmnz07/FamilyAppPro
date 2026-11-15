package albertmnz07.demo.family;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    boolean existsByFamilyAndPersonaAndStatus(Family family, Persona persona, Membership.Status status);

    Optional<Membership> findByFamilyAndPersona(Family family, Persona persona);
}
