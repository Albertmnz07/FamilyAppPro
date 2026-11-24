package albertmnz07.familyapp.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InviteJpaRepository extends JpaRepository<InviteJpaEntity, UUID> {

    boolean existsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InviteJpaEntity i where i.code = :code")
    Optional<InviteJpaEntity> findByCodeForUpdate(@Param("code") String code);
}
