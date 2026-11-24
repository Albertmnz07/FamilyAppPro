package albertmnz07.familyapp.domain.model;

import albertmnz07.familyapp.domain.valueobject.Dni;
import albertmnz07.familyapp.domain.valueobject.DniNumber;
import albertmnz07.familyapp.domain.valueobject.DniPhoto;
import albertmnz07.familyapp.domain.valueobject.PasswordHash;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import albertmnz07.familyapp.domain.valueobject.Username;
import java.util.Objects;
import java.util.Optional;

/** Aggregate Root: a person identified by a unique DNI, able to authenticate and share photos. */
public final class Persona {

    private final PersonaId id;
    private final Username username;
    private final PasswordHash passwordHash;
    private Dni dni;

    private Persona(PersonaId id, Username username, Dni dni, PasswordHash passwordHash) {
        this.id = Objects.requireNonNull(id, "id");
        this.username = Objects.requireNonNull(username, "username");
        this.dni = Objects.requireNonNull(dni, "dni");
        this.passwordHash = passwordHash;
    }

    public static Persona register(
            PersonaId id, Username username, DniNumber dniNumber, PasswordHash passwordHash) {
        Objects.requireNonNull(passwordHash, "passwordHash");
        return new Persona(id, username, Dni.withoutPhotos(dniNumber), passwordHash);
    }

    /** Rebuilds a persona from its stored state; the password hash may be absent. */
    public static Persona restore(
            PersonaId id, Username username, Dni dni, PasswordHash passwordHash) {
        return new Persona(id, username, dni, passwordHash);
    }

    /** Replaces the given sides of the DNI photos; a {@code null} side is left as it was. */
    public void changeDniPhotos(DniPhoto front, DniPhoto back) {
        this.dni = dni.withPhotos(front, back);
    }

    public PersonaId id() {
        return id;
    }

    public Username username() {
        return username;
    }

    public Dni dni() {
        return dni;
    }

    public String dniValue() {
        return dni.value();
    }

    public Optional<PasswordHash> passwordHash() {
        return Optional.ofNullable(passwordHash);
    }
}
