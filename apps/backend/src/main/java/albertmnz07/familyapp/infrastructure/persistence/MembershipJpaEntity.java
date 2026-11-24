package albertmnz07.familyapp.infrastructure.persistence;

import albertmnz07.familyapp.domain.model.Membership;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(
        name = "membership",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_membership_family_persona",
                        columnNames = {"family_id", "persona_id"}))
public class MembershipJpaEntity {

    @Id private UUID id;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "persona_id", nullable = false)
    private UUID personaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Membership.Status status;

    protected MembershipJpaEntity() {}

    public MembershipJpaEntity(UUID id, UUID familyId, UUID personaId, Membership.Status status) {
        this.id = id;
        this.familyId = familyId;
        this.personaId = personaId;
        this.status = status;
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

    public Membership.Status getStatus() {
        return status;
    }
}
