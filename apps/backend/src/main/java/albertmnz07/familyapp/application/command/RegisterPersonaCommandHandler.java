package albertmnz07.familyapp.application.command;

import albertmnz07.familyapp.application.port.PasswordHasher;
import albertmnz07.familyapp.application.query.PersonaView;
import albertmnz07.familyapp.domain.exception.DuplicateDniException;
import albertmnz07.familyapp.domain.exception.DuplicateUsernameException;
import albertmnz07.familyapp.domain.model.Persona;
import albertmnz07.familyapp.domain.repository.PersonaRepository;
import albertmnz07.familyapp.domain.valueobject.DniNumber;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import albertmnz07.familyapp.domain.valueobject.RawPassword;
import albertmnz07.familyapp.domain.valueobject.Username;

public class RegisterPersonaCommandHandler
        implements CommandHandler<RegisterPersonaCommand, PersonaView> {

    private final PersonaRepository personas;
    private final PasswordHasher passwordHasher;

    public RegisterPersonaCommandHandler(
            PersonaRepository personas, PasswordHasher passwordHasher) {
        this.personas = personas;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public PersonaView handle(RegisterPersonaCommand command) {
        Username username = Username.of(command.username());
        DniNumber dni = DniNumber.of(command.dni());
        RawPassword password = new RawPassword(command.password());
        if (personas.existsByUsername(username)) {
            throw new DuplicateUsernameException("Username already taken: " + username.value());
        }
        if (personas.existsByDni(dni)) {
            throw new DuplicateDniException("DNI already registered: " + dni.value());
        }
        Persona persona =
                Persona.register(
                        PersonaId.generate(), username, dni, passwordHasher.hash(password));
        return PersonaView.of(personas.save(persona));
    }
}
