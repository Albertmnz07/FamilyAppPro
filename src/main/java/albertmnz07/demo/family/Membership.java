package albertmnz07.demo.family;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_membership_family_persona",
        columnNames = {"family_id", "persona_id"}))
public class Membership {

    public enum Status {
        ACCEPTED,
        REMOVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    private Family family;

    @ManyToOne(optional = false)
    private Persona persona;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    protected Membership() {
    }

    public Membership(Family family, Persona persona) {
        this.family = family;
        this.persona = persona;
        this.status = Status.ACCEPTED;
    }

    public void accept() {
        this.status = Status.ACCEPTED;
    }

    public void remove() {
        this.status = Status.REMOVED;
    }

    public UUID getId() {
        return id;
    }

    public Family getFamily() {
        return family;
    }

    public Persona getPersona() {
        return persona;
    }

    public Status getStatus() {
        return status;
    }
}
