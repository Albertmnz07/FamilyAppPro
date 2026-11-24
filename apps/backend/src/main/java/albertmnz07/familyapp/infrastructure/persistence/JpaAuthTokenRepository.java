package albertmnz07.familyapp.infrastructure.persistence;

import albertmnz07.familyapp.domain.model.AuthToken;
import albertmnz07.familyapp.domain.repository.AuthTokenRepository;
import albertmnz07.familyapp.domain.valueobject.AuthTokenId;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import albertmnz07.familyapp.domain.valueobject.TokenValue;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAuthTokenRepository implements AuthTokenRepository {

    private final AuthTokenJpaRepository jpa;

    public JpaAuthTokenRepository(AuthTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public AuthToken save(AuthToken token) {
        return toDomain(
                jpa.save(
                        new AuthTokenJpaEntity(
                                token.id().value(),
                                token.value().value(),
                                token.personaId().value(),
                                token.expiresAt(),
                                token.createdAt())));
    }

    @Override
    public Optional<AuthToken> findByValue(TokenValue value) {
        return jpa.findByToken(value.value()).map(JpaAuthTokenRepository::toDomain);
    }

    private static AuthToken toDomain(AuthTokenJpaEntity entity) {
        return AuthToken.restore(
                new AuthTokenId(entity.getId()),
                new TokenValue(entity.getToken()),
                new PersonaId(entity.getPersonaId()),
                entity.getExpiresAt(),
                entity.getCreatedAt());
    }
}
