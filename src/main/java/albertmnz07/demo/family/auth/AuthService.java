package albertmnz07.demo.family.auth;

import albertmnz07.demo.family.Persona;
import albertmnz07.demo.family.PersonaRepository;
import albertmnz07.demo.family.error.UnauthorizedException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Duration TOKEN_TTL = Duration.ofDays(7);

    private final PersonaRepository personas;
    private final AuthTokenRepository tokens;
    private final PasswordHasher passwordHasher = new PasswordHasher();

    public AuthService(PersonaRepository personas, AuthTokenRepository tokens) {
        this.personas = personas;
        this.tokens = tokens;
    }

    @Transactional
    public AuthToken login(String rawUsername, String password) {
        Persona persona =
                personas.findByUsername(normalizeUsername(rawUsername))
                        .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!passwordHasher.matches(password, persona.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        return tokens.save(
                new AuthToken(
                        UUID.randomUUID().toString(), persona, Instant.now().plus(TOKEN_TTL)));
    }

    public Persona resolve(String token) {
        AuthToken authToken =
                tokens.findByToken(token)
                        .orElseThrow(() -> new UnauthorizedException("Unknown token"));
        if (authToken.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Expired token");
        }
        return authToken.getPersona();
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
