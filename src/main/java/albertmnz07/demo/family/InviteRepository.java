package albertmnz07.demo.family;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface InviteRepository extends JpaRepository<Invite, UUID> {

    boolean existsByCode(String code);
}
