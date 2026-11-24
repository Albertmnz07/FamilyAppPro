package albertmnz07.familyapp.domain.model;

import albertmnz07.familyapp.domain.valueobject.ConsentId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Internal Entity owned by {@link Membership}: records a persona's per-family consent to share DNI
 * data (ADR-0001, ADR-0010). Mutations are package-private so consent can only change through its
 * owning {@link Membership} aggregate.
 */
public final class Consent {

    private final ConsentId id;
    private Instant grantedAt;
    private Instant revokedAt;

    private Consent(ConsentId id, Instant grantedAt, Instant revokedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.grantedAt = Objects.requireNonNull(grantedAt, "grantedAt");
        this.revokedAt = revokedAt;
    }

    static Consent grantNew(Instant now) {
        return new Consent(ConsentId.generate(), now, null);
    }

    public static Consent restore(ConsentId id, Instant grantedAt, Instant revokedAt) {
        return new Consent(id, grantedAt, revokedAt);
    }

    public boolean isInForce() {
        return revokedAt == null;
    }

    void grant(Instant now) {
        this.grantedAt = Objects.requireNonNull(now, "now");
        this.revokedAt = null;
    }

    void revoke(Instant now) {
        Objects.requireNonNull(now, "now");
        if (isInForce()) {
            this.revokedAt = now;
        }
    }

    public ConsentId id() {
        return id;
    }

    public Instant grantedAt() {
        return grantedAt;
    }

    public Optional<Instant> revokedAt() {
        return Optional.ofNullable(revokedAt);
    }
}
