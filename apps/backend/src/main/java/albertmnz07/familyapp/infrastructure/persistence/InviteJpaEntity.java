package albertmnz07.familyapp.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invite")
public class InviteJpaEntity {

    @Id private UUID id;

    @Column(nullable = false, unique = true, length = 6)
    private String code;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant redeemedAt;

    protected InviteJpaEntity() {}

    public InviteJpaEntity(
            UUID id, String code, UUID familyId, Instant expiresAt, Instant redeemedAt) {
        this.id = id;
        this.code = code;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
        this.redeemedAt = redeemedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRedeemedAt() {
        return redeemedAt;
    }
}
