package albertmnz07.familyapp.domain.model;

import albertmnz07.familyapp.domain.exception.UnauthorizedException;
import albertmnz07.familyapp.domain.valueobject.AuthTokenId;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import albertmnz07.familyapp.domain.valueobject.TokenValue;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Aggregate Root: an opaque bearer token issued to a persona at login (ADR-0004). */
public final class AuthToken {

    private final AuthTokenId id;
    private final TokenValue value;
    private final PersonaId personaId;
    private final Instant expiresAt;
    private final Instant createdAt;

    private AuthToken(
            AuthTokenId id,
            TokenValue value,
            PersonaId personaId,
            Instant expiresAt,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.value = Objects.requireNonNull(value, "value");
        this.personaId = Objects.requireNonNull(personaId, "personaId");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static AuthToken issue(PersonaId personaId, Instant now, Duration timeToLive) {
        return new AuthToken(
                AuthTokenId.generate(),
                TokenValue.generate(),
                personaId,
                now.plus(timeToLive),
                now);
    }

    public static AuthToken restore(
            AuthTokenId id,
            TokenValue value,
            PersonaId personaId,
            Instant expiresAt,
            Instant createdAt) {
        return new AuthToken(id, value, personaId, expiresAt, createdAt);
    }

    public boolean isExpiredAt(Instant now) {
        return expiresAt.isBefore(now);
    }

    /** Refuses the token with {@code unauthorized} once its expiry has passed. */
    public void requireActiveAt(Instant now) {
        if (isExpiredAt(now)) {
            throw new UnauthorizedException("Expired token");
        }
    }

    public AuthTokenId id() {
        return id;
    }

    public TokenValue value() {
        return value;
    }

    public PersonaId personaId() {
        return personaId;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
