package albertmnz07.familyapp.infrastructure.persistence;

import albertmnz07.familyapp.domain.model.Membership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipJpaRepository extends JpaRepository<MembershipJpaEntity, UUID> {

    Optional<MembershipJpaEntity> findByFamilyIdAndPersonaId(UUID familyId, UUID personaId);

    boolean existsByFamilyIdAndPersonaIdAndStatus(
            UUID familyId, UUID personaId, Membership.Status status);

    List<MembershipJpaEntity> findByPersonaIdAndStatus(UUID personaId, Membership.Status status);

    List<MembershipJpaEntity> findByFamilyIdAndStatus(UUID familyId, Membership.Status status);
}
