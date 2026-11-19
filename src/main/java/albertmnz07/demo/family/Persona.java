package albertmnz07.demo.family;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

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

    protected Persona() {
    }

    public Persona(String username, String dni, String passwordHash) {
        this(username, dni, null, null, null, null, passwordHash);
    }

    public Persona(String username, String dni, String frontPhotoKey, String backPhotoKey) {
        this(username, dni, frontPhotoKey, frontPhotoKey == null ? null : "image/jpeg",
                backPhotoKey, backPhotoKey == null ? null : "image/jpeg", null);
    }

    public Persona(String username, String dni, String frontPhotoKey, String frontPhotoContentType,
            String backPhotoKey, String backPhotoContentType, String passwordHash) {
        this.username = username;
        this.dni = dni;
        this.frontPhotoKey = frontPhotoKey;
        this.frontPhotoContentType = frontPhotoContentType;
        this.backPhotoKey = backPhotoKey;
        this.backPhotoContentType = backPhotoContentType;
        this.passwordHash = passwordHash;
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

    public boolean hasFrontPhoto() {
        return frontPhotoKey != null && !frontPhotoKey.isBlank();
    }

    public boolean hasBackPhoto() {
        return backPhotoKey != null && !backPhotoKey.isBlank();
    }

    public Dni dniEntity() {
        return new Dni(dni, frontPhotoKey, frontPhotoContentType, backPhotoKey, backPhotoContentType);
    }

    public void changeFrontPhoto(String key, String contentType) {
        this.frontPhotoKey = key;
        this.frontPhotoContentType = contentType;
    }

    public void changeBackPhoto(String key, String contentType) {
        this.backPhotoKey = key;
        this.backPhotoContentType = contentType;
    }
}
