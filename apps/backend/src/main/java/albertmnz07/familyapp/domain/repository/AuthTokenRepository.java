package albertmnz07.familyapp.domain.repository;

import albertmnz07.familyapp.domain.model.AuthToken;
import albertmnz07.familyapp.domain.valueobject.TokenValue;
import java.util.Optional;

public interface AuthTokenRepository {

    AuthToken save(AuthToken token);

    Optional<AuthToken> findByValue(TokenValue value);
}
