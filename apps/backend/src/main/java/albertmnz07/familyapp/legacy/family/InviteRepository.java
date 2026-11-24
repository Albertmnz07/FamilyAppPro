package albertmnz07.familyapp.legacy.family;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface InviteRepository extends JpaRepository<Invite, UUID> {

    boolean existsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Invite> findByCode(String code);
}
