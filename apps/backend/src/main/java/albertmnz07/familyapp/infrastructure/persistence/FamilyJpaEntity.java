package albertmnz07.familyapp.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "family")
public class FamilyJpaEntity {

    @Id private UUID id;

    @Column private String name;

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    protected FamilyJpaEntity() {}

    public FamilyJpaEntity(UUID id, String name, UUID adminId) {
        this.id = id;
        this.name = name;
        this.adminId = adminId;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getAdminId() {
        return adminId;
    }
}
