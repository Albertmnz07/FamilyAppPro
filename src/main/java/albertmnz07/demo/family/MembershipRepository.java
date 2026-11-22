package albertmnz07.demo.family;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    boolean existsByFamilyAndPersonaAndStatus(
            Family family, Persona persona, Membership.Status status);

    Optional<Membership> findByFamilyAndPersona(Family family, Persona persona);

    List<Membership> findByPersonaAndStatus(Persona persona, Membership.Status status);

    List<Membership> findByFamilyAndStatus(Family family, Membership.Status status);
}
