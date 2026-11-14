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

    protected Persona() {
    }

    public Persona(String dni) {
        this.dni = dni;
    }

    public UUID getId() {
        return id;
    }

    public String getDni() {
        return dni;
    }
}
