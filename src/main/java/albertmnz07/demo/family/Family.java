package albertmnz07.demo.family;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.util.UUID;

@Entity
public class Family {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    @ManyToOne(optional = false)
    private Persona admin;

    protected Family() {}

    public Family(String name, Persona admin) {
        this.name = name;
        this.admin = admin;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Persona getAdmin() {
        return admin;
    }
}
