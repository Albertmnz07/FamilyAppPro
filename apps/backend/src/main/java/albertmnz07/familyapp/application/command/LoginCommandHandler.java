package albertmnz07.familyapp.application.command;

import albertmnz07.familyapp.application.port.Clock;
import albertmnz07.familyapp.application.port.PasswordHasher;
import albertmnz07.familyapp.domain.exception.InvalidValueException;
import albertmnz07.familyapp.domain.exception.UnauthorizedException;
import albertmnz07.familyapp.domain.model.AuthToken;
import albertmnz07.familyapp.domain.model.Persona;
import albertmnz07.familyapp.domain.repository.AuthTokenRepository;
import albertmnz07.familyapp.domain.repository.PersonaRepository;
import albertmnz07.familyapp.domain.valueobject.RawPassword;
import albertmnz07.familyapp.domain.valueobject.Username;
import java.time.Duration;

public class LoginCommandHandler implements CommandHandler<LoginCommand, LoginResult> {

    static final Duration TOKEN_TTL = Duration.ofDays(7);

    private final PersonaRepository personas;
    private final AuthTokenRepository tokens;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public LoginCommandHandler(
            PersonaRepository personas,
            AuthTokenRepository tokens,
            PasswordHasher passwordHasher,
            Clock clock) {
        this.personas = personas;
        this.tokens = tokens;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    public LoginResult handle(LoginCommand command) {
        Persona persona =
                personas.findByUsername(parseUsername(command.username()))
                        .orElseThrow(LoginCommandHandler::invalidCredentials);
        RawPassword password = parsePassword(command.password());
        boolean matches =
                persona.passwordHash()
                        .filter(hash -> passwordHasher.matches(password, hash))
                        .isPresent();
        if (!matches) {
            throw invalidCredentials();
        }
        AuthToken token = tokens.save(AuthToken.issue(persona.id(), clock.now(), TOKEN_TTL));
        return new LoginResult(
                token.value().value(), persona.id().value(), persona.username().value());
    }

    private static Username parseUsername(String raw) {
        try {
            return Username.of(raw);
        } catch (InvalidValueException malformed) {
            throw invalidCredentials();
        }
    }

    private static RawPassword parsePassword(String raw) {
        try {
            return new RawPassword(raw);
        } catch (InvalidValueException malformed) {
            throw invalidCredentials();
        }
    }

    private static UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("Invalid credentials");
    }
}
