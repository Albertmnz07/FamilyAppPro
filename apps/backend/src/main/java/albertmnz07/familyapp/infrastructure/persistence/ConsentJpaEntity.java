package albertmnz07.familyapp.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "consent",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_consent_family_persona",
                        columnNames = {"family_id", "persona_id"}))
public class ConsentJpaEntity {

    @Id private UUID id;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "persona_id", nullable = false)
    private UUID personaId;

    @Column(nullable = false)
    private Instant grantedAt;

    private Instant revokedAt;

    protected ConsentJpaEntity() {}

    public ConsentJpaEntity(
            UUID id, UUID familyId, UUID personaId, Instant grantedAt, Instant revokedAt) {
        this.id = id;
        this.familyId = familyId;
        this.personaId = personaId;
        this.grantedAt = grantedAt;
        this.revokedAt = revokedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public UUID getPersonaId() {
        return personaId;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
