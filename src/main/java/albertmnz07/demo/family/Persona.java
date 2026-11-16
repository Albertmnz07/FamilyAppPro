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

    @Column(nullable = false, length = 1_048_576)
    private byte[] frontPhoto;

    @Column(nullable = false)
    private String frontPhotoContentType;

    @Column(nullable = false, length = 1_048_576)
    private byte[] backPhoto;

    @Column(nullable = false)
    private String backPhotoContentType;

    protected Persona() {
    }

    public Persona(String username, String dni, byte[] frontPhoto, byte[] backPhoto) {
        this(username, dni, frontPhoto, "image/jpeg", backPhoto, "image/jpeg", null);
    }

    public Persona(String username, String dni, byte[] frontPhoto, String frontPhotoContentType, byte[] backPhoto,
            String backPhotoContentType, String passwordHash) {
        this.username = username;
        this.dni = dni;
        this.frontPhoto = frontPhoto;
        this.frontPhotoContentType = frontPhotoContentType;
        this.backPhoto = backPhoto;
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

    public byte[] getFrontPhoto() {
        return frontPhoto;
    }

    public String getFrontPhotoContentType() {
        return frontPhotoContentType;
    }

    public byte[] getBackPhoto() {
        return backPhoto;
    }

    public String getBackPhotoContentType() {
        return backPhotoContentType;
    }

    public Dni dniEntity() {
        return new Dni(dni, frontPhoto, frontPhotoContentType, backPhoto, backPhotoContentType);
    }
}
