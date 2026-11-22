package albertmnz07.demo.family;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import albertmnz07.demo.family.error.PhotoTooLargeException;
import albertmnz07.demo.family.error.UnsupportedPhotoTypeException;
import org.junit.jupiter.api.Test;

class PersonaServicePhotoTest {

    private static final int TEN_MB = 10 * 1024 * 1024;

    @Test
    void acceptsAPhotoAtTheTenMegabyteLimit() {
        assertThatCode(() -> PersonaService.validatePhoto(new byte[TEN_MB], "image/jpeg", "front"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAPhotoAboveTheTenMegabyteLimit() {
        assertThatThrownBy(() ->
                        PersonaService.validatePhoto(new byte[TEN_MB + 1], "image/png", "back"))
                .isInstanceOf(PhotoTooLargeException.class)
                .hasMessageContaining("10 MB");
    }

    @Test
    void rejectsAnUnsupportedPhotoType() {
        assertThatThrownBy(
                        () -> PersonaService.validatePhoto(new byte[] {1}, "text/plain", "front"))
                .isInstanceOf(UnsupportedPhotoTypeException.class);
    }
}
