package albertmnz07.familyapp.legacy.family;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.util.UUID;

@Entity
public class Invite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 6)
    private String code;

    @ManyToOne(optional = false)
    private Family family;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant redeemedAt;

    protected Invite() {}

    public Invite(String code, Family family, Instant expiresAt) {
        this.code = code;
        this.family = family;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public Family getFamily() {
        return family;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isRedeemed() {
        return redeemedAt != null;
    }

    public void redeem(Instant now) {
        this.redeemedAt = now;
    }
}
