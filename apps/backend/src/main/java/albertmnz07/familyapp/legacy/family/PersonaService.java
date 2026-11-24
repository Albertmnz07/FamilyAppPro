package albertmnz07.familyapp.legacy.family;

import albertmnz07.familyapp.legacy.family.auth.PasswordHasher;
import albertmnz07.familyapp.legacy.family.error.DuplicateDniException;
import albertmnz07.familyapp.legacy.family.error.DuplicateUsernameException;
import albertmnz07.familyapp.legacy.family.error.NotFoundException;
import albertmnz07.familyapp.legacy.family.error.PhotoTooLargeException;
import albertmnz07.familyapp.legacy.family.error.UnsupportedPhotoTypeException;
import albertmnz07.familyapp.legacy.family.photo.PhotoStore;
import albertmnz07.familyapp.legacy.family.photo.PhotoUpload;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonaService {

    private static final Set<String> ALLOWED_PHOTO_TYPES = Set.of("image/jpeg", "image/png");
    private static final int MAX_PHOTO_BYTES = 10 * 1024 * 1024;
    private static final int MIN_PASSWORD_LENGTH = 3;
    private static final int MAX_PASSWORD_LENGTH = 100;
    private static final String USERNAME_PATTERN = "[a-z0-9._-]{3,30}";

    private final PersonaRepository personas;
    private final PhotoStore photos;
    private final PasswordHasher passwordHasher = new PasswordHasher();

    public PersonaService(PersonaRepository personas, PhotoStore photos) {
        this.personas = personas;
        this.photos = photos;
    }

    @Transactional
    public Persona register(String rawUsername, String rawDni, String password) {
        String username = normalizeUsername(rawUsername);
        String dni = normalizeDni(rawDni);
        validatePassword(password);
        if (personas.existsByUsername(username)) {
            throw new DuplicateUsernameException("Username already taken: " + username);
        }
        if (personas.existsByDni(dni)) {
            throw new DuplicateDniException("DNI already registered: " + dni);
        }
        return personas.save(new Persona(username, dni, passwordHasher.hash(password)));
    }

    public Persona find(UUID id) {
        return personas.findById(id)
                .orElseThrow(() -> new NotFoundException("Persona not found: " + id));
    }

    /**
     * Uploads or replaces the acting Persona's optional DNI photos. Each side is independent: an
     * absent side is left as it was. Both sides are validated before either is stored, so a
     * rejected side leaves the other untouched.
     */
    @Transactional
    public Persona updateDniPhotos(Persona persona, PhotoUpload front, PhotoUpload back) {
        if (front != null) {
            validatePhoto(front.content(), front.contentType(), "front");
        }
        if (back != null) {
            validatePhoto(back.content(), back.contentType(), "back");
        }
        if (front != null) {
            persona.changeFrontPhoto(photos.put(front.content()), front.contentType());
        }
        if (back != null) {
            persona.changeBackPhoto(photos.put(back.content()), back.contentType());
        }
        return personas.save(persona);
    }

    private String normalizeDni(String rawDni) {
        return rawDni.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeUsername(String rawUsername) {
        String username = rawUsername.trim().toLowerCase(Locale.ROOT);
        if (!username.matches(USERNAME_PATTERN)) {
            throw new IllegalArgumentException(
                    "The username must be 3-30 characters: letters, digits, dots, underscores or hyphens");
        }
        return username;
    }

    private void validatePassword(String password) {
        if (password == null
                || password.length() < MIN_PASSWORD_LENGTH
                || password.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("The password must be 3-100 characters");
        }
    }

    public static void validatePhoto(byte[] photo, String contentType, String side) {
        if (photo == null || photo.length == 0) {
            throw new IllegalArgumentException("The " + side + " photo is required");
        }
        if (photo.length > MAX_PHOTO_BYTES) {
            throw new PhotoTooLargeException("The " + side + " photo exceeds 10 MB");
        }
        if (contentType == null
                || !ALLOWED_PHOTO_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new UnsupportedPhotoTypeException(
                    "The " + side + " photo must be image/jpeg or image/png");
        }
    }
}
