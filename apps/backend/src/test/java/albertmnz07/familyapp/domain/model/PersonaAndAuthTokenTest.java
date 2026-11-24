package albertmnz07.familyapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import albertmnz07.familyapp.domain.exception.UnauthorizedException;
import albertmnz07.familyapp.domain.valueobject.DniNumber;
import albertmnz07.familyapp.domain.valueobject.DniPhoto;
import albertmnz07.familyapp.domain.valueobject.PasswordHash;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import albertmnz07.familyapp.domain.valueobject.PhotoContentType;
import albertmnz07.familyapp.domain.valueobject.PhotoKey;
import albertmnz07.familyapp.domain.valueobject.Username;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PersonaAndAuthTokenTest {

    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    private static DniPhoto photo(String key) {
        return new DniPhoto(new PhotoKey(key), PhotoContentType.of("image/png"));
    }

    private static Persona persona() {
        return Persona.register(
                PersonaId.generate(),
                Username.of("alice"),
                DniNumber.of("1a"),
                new PasswordHash("hash"));
    }

    @Test
    void aRegisteredPersonaHasNoPhotosAndKeepsItsHash() {
        Persona persona = persona();

        assertThat(persona.dni().number().value()).isEqualTo("1A");
        assertThat(persona.dni().front()).isEmpty();
        assertThat(persona.dni().back()).isEmpty();
        assertThat(persona.passwordHash()).contains(new PasswordHash("hash"));
    }

    @Test
    void changingOnePhotoPreservesTheOmittedSide() {
        Persona persona = persona();

        persona.changeDniPhotos(photo("front-1"), photo("back-1"));
        persona.changeDniPhotos(photo("front-2"), null);

        assertThat(persona.dni().front()).contains(photo("front-2"));
        assertThat(persona.dni().back()).contains(photo("back-1"));
    }

    @Test
    void aRestoredLegacyPersonaMayHaveNoPassword() {
        Persona persona =
                Persona.restore(
                        PersonaId.generate(),
                        Username.of("legacy"),
                        albertmnz07.familyapp.domain.valueobject.Dni.withoutPhotos(
                                DniNumber.of("2")),
                        null);

        assertThat(persona.passwordHash()).isEmpty();
    }

    @Test
    void anIssuedTokenExpiresAfterItsTimeToLive() {
        AuthToken token = AuthToken.issue(PersonaId.generate(), NOW, Duration.ofDays(7));

        assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        assertThatCode(() -> token.requireActiveAt(NOW.plus(Duration.ofDays(7))))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> token.requireActiveAt(NOW.plus(Duration.ofDays(7)).plusSeconds(1)))
                .isInstanceOf(UnauthorizedException.class);
    }
}
