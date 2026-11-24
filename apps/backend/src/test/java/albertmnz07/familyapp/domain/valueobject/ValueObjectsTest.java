package albertmnz07.familyapp.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import albertmnz07.familyapp.domain.exception.InvalidValueException;
import albertmnz07.familyapp.domain.exception.PhotoTooLargeException;
import albertmnz07.familyapp.domain.exception.UnsupportedPhotoTypeException;
import org.junit.jupiter.api.Test;

class ValueObjectsTest {

    private static final int TEN_MB = 10 * 1024 * 1024;

    @Test
    void usernameIsTrimmedAndLowercased() {
        assertThat(Username.of("  Alice.B_1-x ").value()).isEqualTo("alice.b_1-x");
    }

    @Test
    void usernameRejectsTooShortTooLongAndForbiddenCharacters() {
        assertThatThrownBy(() -> Username.of("ab")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> Username.of("a".repeat(31)))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> Username.of("bad name")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> Username.of(null)).isInstanceOf(InvalidValueException.class);
        assertThatCode(() -> Username.of("a".repeat(30))).doesNotThrowAnyException();
    }

    @Test
    void dniNumberIsTrimmedUppercasedAndAcceptsNonSpanishFormats() {
        assertThat(DniNumber.of(" 12345678z ").value()).isEqualTo("12345678Z");
        assertThat(DniNumber.of("dni-sin-formato").value()).isEqualTo("DNI-SIN-FORMATO");
    }

    @Test
    void dniNumberRejectsBlankAndOversizedValues() {
        assertThatThrownBy(() -> DniNumber.of("   ")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> DniNumber.of(null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> DniNumber.of("a".repeat(256)))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void rawPasswordEnforcesLengthAndNeverPrintsItself() {
        assertThatCode(() -> new RawPassword("abc")).doesNotThrowAnyException();
        assertThatCode(() -> new RawPassword("a".repeat(100))).doesNotThrowAnyException();
        assertThatThrownBy(() -> new RawPassword("ab")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new RawPassword("a".repeat(101)))
                .isInstanceOf(InvalidValueException.class);
        assertThat(new RawPassword("secret").toString()).doesNotContain("secret");
    }

    @Test
    void passwordHashAndTokenValueRejectBlank() {
        assertThatThrownBy(() -> new PasswordHash(" ")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new TokenValue("")).isInstanceOf(InvalidValueException.class);
        assertThat(TokenValue.generate()).isNotEqualTo(TokenValue.generate());
    }

    @Test
    void typedAggregateAndEntityIdsGenerateDistinctNonNullUuids() {
        PersonaId personaId = PersonaId.generate();
        FamilyId familyId = FamilyId.generate();
        InviteId inviteId = InviteId.generate();
        MembershipId membershipId = MembershipId.generate();
        ConsentId consentId = ConsentId.generate();
        AuthTokenId authTokenId = AuthTokenId.generate();

        assertThat(new PersonaId(personaId.value())).isEqualTo(personaId);
        assertThat(new FamilyId(familyId.value())).isEqualTo(familyId);
        assertThat(new InviteId(inviteId.value())).isEqualTo(inviteId);
        assertThat(new MembershipId(membershipId.value())).isEqualTo(membershipId);
        assertThat(new ConsentId(consentId.value())).isEqualTo(consentId);
        assertThat(new AuthTokenId(authTokenId.value())).isEqualTo(authTokenId);

        assertThatThrownBy(() -> new PersonaId(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new FamilyId(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new InviteId(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new MembershipId(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ConsentId(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AuthTokenId(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void photoKeyRejectsBlank() {
        assertThatThrownBy(() -> new PhotoKey(" ")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void photoContentTypeAcceptsOnlyJpegAndPngAndRestoresFallbackWhenMissing() {
        assertThat(PhotoContentType.of("IMAGE/PNG").value()).isEqualTo("image/png");
        assertThat(PhotoContentType.of("IMAGE/JPEG", PhotoSide.FRONT).value())
                .isEqualTo("image/jpeg");
        assertThat(PhotoContentType.restore(null).value()).isEqualTo("image/jpeg");
        assertThat(PhotoContentType.restore("IMAGE/PNG").value()).isEqualTo("image/png");
        assertThatThrownBy(() -> PhotoContentType.of("text/plain"))
                .isInstanceOf(UnsupportedPhotoTypeException.class);
        assertThatThrownBy(() -> PhotoContentType.of(null))
                .isInstanceOf(UnsupportedPhotoTypeException.class);
        assertThatThrownBy(() -> PhotoContentType.of("text/plain", PhotoSide.BACK))
                .isInstanceOf(UnsupportedPhotoTypeException.class)
                .hasMessageContaining("back");
    }

    @Test
    void rawPhotoAcceptsAPhotoAtTheTenMegabyteLimit() {
        assertThatCode(() -> RawPhoto.of(PhotoSide.FRONT, new byte[TEN_MB], "image/jpeg"))
                .doesNotThrowAnyException();
    }

    @Test
    void rawPhotoRejectsAboveTheLimitEmptyAndUnsupportedType() {
        assertThatThrownBy(() -> RawPhoto.of(PhotoSide.BACK, new byte[TEN_MB + 1], "image/png"))
                .isInstanceOf(PhotoTooLargeException.class)
                .hasMessageContaining("back")
                .hasMessageContaining("10 MB");
        assertThatThrownBy(() -> RawPhoto.of(PhotoSide.FRONT, new byte[0], "image/png"))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> RawPhoto.of(PhotoSide.FRONT, new byte[] {1}, "text/plain"))
                .isInstanceOf(UnsupportedPhotoTypeException.class);
    }

    @Test
    void dniComposesNumberAndOptionalPhotosKeepingOmittedSides() {
        DniPhoto front = new DniPhoto(new PhotoKey("f"), PhotoContentType.of("image/png"));
        DniPhoto back = new DniPhoto(new PhotoKey("b"), PhotoContentType.of("image/jpeg"));
        Dni dni = Dni.withoutPhotos(DniNumber.of("X1"));
        assertThat(dni.front()).isEmpty();
        assertThat(dni.photo(PhotoSide.FRONT)).isEmpty();
        assertThat(dni.photo(PhotoSide.BACK)).isEmpty();

        Dni withFront = dni.withPhotos(front, null);
        Dni both = withFront.withPhotos(null, back);

        assertThat(both.front()).contains(front);
        assertThat(both.back()).contains(back);
        assertThat(both.photo(PhotoSide.FRONT)).contains(front);
        assertThat(both.photo(PhotoSide.BACK)).contains(back);
        assertThat(both.number()).isEqualTo(DniNumber.of("X1"));
        assertThat(both.value()).isEqualTo("X1");
    }

    @Test
    void familyNameRequiresNonBlankTextUpToOneHundredCharacters() {
        assertThat(FamilyName.of("Familia García").value()).isEqualTo("Familia García");
        assertThatCode(() -> FamilyName.of("a".repeat(100))).doesNotThrowAnyException();

        assertThatThrownBy(() -> FamilyName.of(null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> FamilyName.of("")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> FamilyName.of("   ")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> FamilyName.of("a".repeat(101)))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void inviteCodeTrimsUppercasesAndRequiresSixAlphanumericCharacters() {
        assertThat(InviteCode.of("  ab12z9 ").value()).isEqualTo("AB12Z9");
        assertThat(InviteCode.of("0O1IL9").value()).isEqualTo("0O1IL9");

        assertThatThrownBy(() -> InviteCode.of(null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> InviteCode.of("ABC12")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> InviteCode.of("ABC1234"))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> InviteCode.of("AB-123")).isInstanceOf(InvalidValueException.class);
    }
}
