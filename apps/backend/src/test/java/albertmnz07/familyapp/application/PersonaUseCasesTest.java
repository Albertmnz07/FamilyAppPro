package albertmnz07.familyapp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import albertmnz07.familyapp.application.command.LoginCommand;
import albertmnz07.familyapp.application.command.LoginCommandHandler;
import albertmnz07.familyapp.application.command.LoginResult;
import albertmnz07.familyapp.application.command.PhotoUpload;
import albertmnz07.familyapp.application.command.RegisterPersonaCommand;
import albertmnz07.familyapp.application.command.RegisterPersonaCommandHandler;
import albertmnz07.familyapp.application.command.UpdateDniPhotosCommand;
import albertmnz07.familyapp.application.command.UpdateDniPhotosCommandHandler;
import albertmnz07.familyapp.application.port.Clock;
import albertmnz07.familyapp.application.port.PasswordHasher;
import albertmnz07.familyapp.application.port.PhotoStore;
import albertmnz07.familyapp.application.query.AuthenticateTokenQuery;
import albertmnz07.familyapp.application.query.AuthenticateTokenQueryHandler;
import albertmnz07.familyapp.application.query.PersonaView;
import albertmnz07.familyapp.domain.exception.DuplicateDniException;
import albertmnz07.familyapp.domain.exception.DuplicateUsernameException;
import albertmnz07.familyapp.domain.exception.PhotoTooLargeException;
import albertmnz07.familyapp.domain.exception.UnauthorizedException;
import albertmnz07.familyapp.domain.model.AuthToken;
import albertmnz07.familyapp.domain.model.Persona;
import albertmnz07.familyapp.domain.repository.AuthTokenRepository;
import albertmnz07.familyapp.domain.repository.PersonaRepository;
import albertmnz07.familyapp.domain.valueobject.DniNumber;
import albertmnz07.familyapp.domain.valueobject.PasswordHash;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import albertmnz07.familyapp.domain.valueobject.PhotoKey;
import albertmnz07.familyapp.domain.valueobject.RawPassword;
import albertmnz07.familyapp.domain.valueobject.RawPhoto;
import albertmnz07.familyapp.domain.valueobject.TokenValue;
import albertmnz07.familyapp.domain.valueobject.Username;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PersonaUseCasesTest {

    private final Map<PersonaId, Persona> personaStore = new HashMap<>();
    private final Map<TokenValue, AuthToken> tokenStore = new HashMap<>();
    private final Map<PhotoKey, byte[]> photoStore = new HashMap<>();
    private Instant now = Instant.parse("2030-01-01T00:00:00Z");

    private final PersonaRepository personas =
            new PersonaRepository() {
                public Persona save(Persona persona) {
                    personaStore.put(persona.id(), persona);
                    return persona;
                }

                public Optional<Persona> findById(PersonaId id) {
                    return Optional.ofNullable(personaStore.get(id));
                }

                public Optional<Persona> findByUsername(Username username) {
                    return personaStore.values().stream()
                            .filter(p -> p.username().equals(username))
                            .findFirst();
                }

                public boolean existsByUsername(Username username) {
                    return findByUsername(username).isPresent();
                }

                public boolean existsByDni(DniNumber dni) {
                    return personaStore.values().stream()
                            .anyMatch(p -> p.dni().number().equals(dni));
                }
            };
    private final AuthTokenRepository tokens =
            new AuthTokenRepository() {
                public AuthToken save(AuthToken token) {
                    tokenStore.put(token.value(), token);
                    return token;
                }

                public Optional<AuthToken> findByValue(TokenValue value) {
                    return Optional.ofNullable(tokenStore.get(value));
                }
            };
    private final PasswordHasher hasher =
            new PasswordHasher() {
                public PasswordHash hash(RawPassword password) {
                    return new PasswordHash("hashed:" + password.value());
                }

                public boolean matches(RawPassword password, PasswordHash hash) {
                    return hash.value().equals("hashed:" + password.value());
                }
            };
    private final PhotoStore photos =
            new PhotoStore() {
                public PhotoKey put(RawPhoto photo) {
                    PhotoKey key = new PhotoKey(UUID.randomUUID().toString());
                    photoStore.put(key, photo.content());
                    return key;
                }

                public Optional<byte[]> get(PhotoKey key) {
                    return Optional.ofNullable(photoStore.get(key));
                }
            };
    private final Clock clock = () -> now;

    private final RegisterPersonaCommandHandler register =
            new RegisterPersonaCommandHandler(personas, hasher);
    private final LoginCommandHandler login =
            new LoginCommandHandler(personas, tokens, hasher, clock);
    private final UpdateDniPhotosCommandHandler updatePhotos =
            new UpdateDniPhotosCommandHandler(personas, photos);
    private final AuthenticateTokenQueryHandler authenticate =
            new AuthenticateTokenQueryHandler(tokens, clock);

    private PersonaView registerAlice() {
        return register.handle(new RegisterPersonaCommand(" Alice ", " 1a ", "secret"));
    }

    @Test
    void registrationNormalizesAndRejectsDuplicates() {
        PersonaView view = registerAlice();

        assertThat(view.username()).isEqualTo("alice");
        assertThat(view.dni()).isEqualTo("1A");
        assertThatThrownBy(
                        () -> register.handle(new RegisterPersonaCommand("alice", "2", "secret")))
                .isInstanceOf(DuplicateUsernameException.class);
        assertThatThrownBy(() -> register.handle(new RegisterPersonaCommand("bob", "1a", "secret")))
                .isInstanceOf(DuplicateDniException.class);
    }

    @Test
    void loginIssuesATokenThatAuthenticatesUntilItExpires() {
        PersonaView view = registerAlice();

        LoginResult result = login.handle(new LoginCommand("ALICE", "secret"));
        PersonaId resolved = authenticate.handle(new AuthenticateTokenQuery(result.token()));

        assertThat(resolved.value()).isEqualTo(view.id());
        now = now.plusSeconds(8 * 24 * 3600);
        assertThatThrownBy(() -> authenticate.handle(new AuthenticateTokenQuery(result.token())))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void loginIsUnauthorizedForBadUsernameFormatUnknownUserOrWrongPassword() {
        registerAlice();

        for (LoginCommand bad :
                new LoginCommand[] {
                    new LoginCommand("!", "secret"),
                    new LoginCommand("nobody", "secret"),
                    new LoginCommand("alice", "wrong"),
                    new LoginCommand("alice", "x")
                }) {
            assertThatThrownBy(() -> login.handle(bad)).isInstanceOf(UnauthorizedException.class);
        }
    }

    @Test
    void unknownOrBlankTokensAreUnauthorized() {
        assertThatThrownBy(() -> authenticate.handle(new AuthenticateTokenQuery("nope")))
                .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> authenticate.handle(new AuthenticateTokenQuery(" ")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void photosAreValidatedBeforeAnySideIsStoredAndOmittedSidesAreKept() {
        PersonaView view = registerAlice();
        PhotoUpload png = new PhotoUpload(new byte[] {1}, "image/png");
        PhotoUpload tooBig = new PhotoUpload(new byte[RawPhoto.MAX_BYTES + 1], "image/png");

        assertThatThrownBy(() ->
                        updatePhotos.handle(new UpdateDniPhotosCommand(view.id(), png, tooBig)))
                .isInstanceOf(PhotoTooLargeException.class);
        assertThat(photoStore).isEmpty();

        PersonaView withFront =
                updatePhotos.handle(new UpdateDniPhotosCommand(view.id(), png, null));
        PersonaView withBoth =
                updatePhotos.handle(
                        new UpdateDniPhotosCommand(
                                view.id(), null, new PhotoUpload(new byte[] {2}, "image/jpeg")));

        assertThat(withFront.hasFrontPhoto()).isTrue();
        assertThat(withFront.hasBackPhoto()).isFalse();
        assertThat(withBoth.hasFrontPhoto()).isTrue();
        assertThat(withBoth.hasBackPhoto()).isTrue();
    }
}
