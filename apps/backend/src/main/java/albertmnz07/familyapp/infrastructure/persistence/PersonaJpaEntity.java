package albertmnz07.familyapp.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "persona")
public class PersonaJpaEntity {

    @Id private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(nullable = false, unique = true)
    private String dni;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "front_photo_key")
    private String frontPhotoKey;

    @Column(name = "front_photo_content_type")
    private String frontPhotoContentType;

    @Column(name = "back_photo_key")
    private String backPhotoKey;

    @Column(name = "back_photo_content_type")
    private String backPhotoContentType;

    protected PersonaJpaEntity() {}

    public PersonaJpaEntity(
            UUID id,
            String username,
            String dni,
            String passwordHash,
            String frontPhotoKey,
            String frontPhotoContentType,
            String backPhotoKey,
            String backPhotoContentType) {
        this.id = id;
        this.username = username;
        this.dni = dni;
        this.passwordHash = passwordHash;
        this.frontPhotoKey = frontPhotoKey;
        this.frontPhotoContentType = frontPhotoContentType;
        this.backPhotoKey = backPhotoKey;
        this.backPhotoContentType = backPhotoContentType;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDni() {
        return dni;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFrontPhotoKey() {
        return frontPhotoKey;
    }

    public String getFrontPhotoContentType() {
        return frontPhotoContentType;
    }

    public String getBackPhotoKey() {
        return backPhotoKey;
    }

    public String getBackPhotoContentType() {
        return backPhotoContentType;
    }
}
