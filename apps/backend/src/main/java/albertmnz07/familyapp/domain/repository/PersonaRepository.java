package albertmnz07.familyapp.domain.repository;

import albertmnz07.familyapp.domain.model.Persona;
import albertmnz07.familyapp.domain.valueobject.DniNumber;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import albertmnz07.familyapp.domain.valueobject.Username;
import java.util.Optional;

public interface PersonaRepository {

    Persona save(Persona persona);

    Optional<Persona> findById(PersonaId id);

    Optional<Persona> findByUsername(Username username);

    boolean existsByUsername(Username username);

    boolean existsByDni(DniNumber dni);
}
