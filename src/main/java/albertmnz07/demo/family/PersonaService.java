package albertmnz07.demo.family;

import albertmnz07.demo.family.error.DuplicateDniException;
import albertmnz07.demo.family.error.DuplicateUsernameException;
import albertmnz07.demo.family.error.NotFoundException;
import albertmnz07.demo.family.error.UnsupportedPhotoTypeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class PersonaService {

    private static final Set<String> ALLOWED_PHOTO_TYPES = Set.of("image/jpeg", "image/png");
    private static final int MAX_PHOTO_BYTES = 1_048_576;
    private static final String USERNAME_PATTERN = "[a-z0-9._-]{3,30}";

    private final PersonaRepository personas;

    public PersonaService(PersonaRepository personas) {
        this.personas = personas;
    }

    @Transactional
    public Persona register(String rawUsername, String rawDni, byte[] frontPhoto, String frontPhotoContentType,
            byte[] backPhoto, String backPhotoContentType) {
        String username = normalizeUsername(rawUsername);
        String dni = normalizeDni(rawDni);
        if (personas.existsByUsername(username)) {
            throw new DuplicateUsernameException("Username already taken: " + username);
        }
        if (personas.existsByDni(dni)) {
            throw new DuplicateDniException("DNI already registered: " + dni);
        }
        validatePhoto(frontPhoto, frontPhotoContentType, "front");
        validatePhoto(backPhoto, backPhotoContentType, "back");
        return personas.save(new Persona(username, dni, frontPhoto, frontPhotoContentType, backPhoto,
                backPhotoContentType));
    }

    public Persona find(UUID id) {
        return personas.findById(id)
                .orElseThrow(() -> new NotFoundException("Persona not found: " + id));
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

    private void validatePhoto(byte[] photo, String contentType, String side) {
        if (photo == null || photo.length == 0) {
            throw new IllegalArgumentException("The " + side + " photo is required");
        }
        if (photo.length > MAX_PHOTO_BYTES) {
            throw new IllegalArgumentException("The " + side + " photo exceeds 1 MB");
        }
        if (contentType == null || !ALLOWED_PHOTO_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new UnsupportedPhotoTypeException(
                    "The " + side + " photo must be image/jpeg or image/png");
        }
    }
}
