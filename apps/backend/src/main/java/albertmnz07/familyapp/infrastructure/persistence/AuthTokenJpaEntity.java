package albertmnz07.familyapp.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_token")
public class AuthTokenJpaEntity {

    @Id private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "persona_id", nullable = false)
    private UUID personaId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuthTokenJpaEntity() {}

    public AuthTokenJpaEntity(
            UUID id, String token, UUID personaId, Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.token = token;
        this.personaId = personaId;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public UUID getPersonaId() {
        return personaId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
