package albertmnz07.familyapp.domain.valueobject;

import albertmnz07.familyapp.domain.exception.InvalidValueException;
import java.util.Locale;
import java.util.regex.Pattern;

/** A persona's unique lowercase handle: 3-30 letters, digits, dots, underscores or hyphens. */
public record Username(String value) {

    private static final Pattern PATTERN = Pattern.compile("[a-z0-9._-]{3,30}");

    public Username {
        if (value == null || !PATTERN.matcher(value).matches()) {
            throw new InvalidValueException(
                    "The username must be 3-30 characters: letters, digits, dots, underscores or hyphens");
        }
    }

    /** Trims and lowercases the raw text before validating it. */
    public static Username of(String raw) {
        if (raw == null) {
            return new Username(null);
        }
        return new Username(raw.trim().toLowerCase(Locale.ROOT));
    }
}
