package albertmnz07.familyapp.legacy.family.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {

    @Query("select t from AuthToken t join fetch t.persona where t.token = :token")
    Optional<AuthToken> findByToken(@Param("token") String token);
}
