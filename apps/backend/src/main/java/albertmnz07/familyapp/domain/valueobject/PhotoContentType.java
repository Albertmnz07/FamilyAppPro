package albertmnz07.familyapp.domain.valueobject;

import albertmnz07.familyapp.domain.exception.UnsupportedPhotoTypeException;
import java.util.Locale;
import java.util.Set;

/** The media type of a DNI photo: image/jpeg or image/png. */
public record PhotoContentType(String value) {

    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png");
    private static final String FALLBACK = "image/jpeg";

    public PhotoContentType {
        if (value == null || !ALLOWED.contains(value)) {
            throw new UnsupportedPhotoTypeException("The photo must be image/jpeg or image/png");
        }
    }

    public static PhotoContentType of(String raw) {
        return of(raw, "photo");
    }

    /** Parses an uploaded type, naming the photo side in the refusal message. */
    public static PhotoContentType of(String raw, String side) {
        String normalized = raw == null ? null : raw.toLowerCase(Locale.ROOT);
        if (normalized == null || !ALLOWED.contains(normalized)) {
            throw new UnsupportedPhotoTypeException(
                    "The " + side + " photo must be image/jpeg or image/png");
        }
        return new PhotoContentType(normalized);
    }

    /** Rebuilds the type of an already stored photo, tolerating legacy or missing values. */
    public static PhotoContentType restore(String stored) {
        String normalized = stored == null ? null : stored.toLowerCase(Locale.ROOT);
        return new PhotoContentType(
                normalized != null && ALLOWED.contains(normalized) ? normalized : FALLBACK);
    }
}
