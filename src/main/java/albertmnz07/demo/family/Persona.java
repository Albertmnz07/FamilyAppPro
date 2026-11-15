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

    @Column(nullable = false, unique = true)
    private String dni;

    @Column(nullable = false, length = 1_048_576)
    private byte[] frontPhoto;

    @Column(nullable = false, length = 1_048_576)
    private byte[] backPhoto;

    protected Persona() {
    }

    public Persona(String dni, byte[] frontPhoto, byte[] backPhoto) {
        this.dni = dni;
        this.frontPhoto = frontPhoto;
        this.backPhoto = backPhoto;
    }

    public UUID getId() {
        return id;
    }

    public String getDni() {
        return dni;
    }

    public byte[] getFrontPhoto() {
        return frontPhoto;
    }

    public byte[] getBackPhoto() {
        return backPhoto;
    }

    public Dni dniEntity() {
        return new Dni(dni, frontPhoto, backPhoto);
    }
}
