package albertmnz07.demo.family.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {

    @Query("select t from AuthToken t join fetch t.persona where t.token = :token")
    Optional<AuthToken> findByToken(@Param("token") String token);
}
