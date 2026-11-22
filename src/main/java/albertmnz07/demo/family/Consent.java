package albertmnz07.demo.family;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_consent_family_persona",
                        columnNames = {"family_id", "persona_id"}))
public class Consent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    private Family family;

    @ManyToOne(optional = false)
    private Persona persona;

    @Column(nullable = false)
    private Instant grantedAt;

    private Instant revokedAt;

    protected Consent() {}

    public Consent(Family family, Persona persona, Instant grantedAt) {
        this.family = family;
        this.persona = persona;
        this.grantedAt = grantedAt;
    }

    public boolean isInForce() {
        return revokedAt == null;
    }

    public void grant(Instant now) {
        this.grantedAt = now;
        this.revokedAt = null;
    }

    public void revoke(Instant now) {
        this.revokedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public Family getFamily() {
        return family;
    }

    public Persona getPersona() {
        return persona;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
