package albertmnz07.familyapp.domain.valueobject;

import albertmnz07.familyapp.domain.exception.InvalidValueException;
import java.util.Locale;
import java.util.regex.Pattern;

/** A single-use 6-character uppercase alphanumeric invite token. */
public record InviteCode(String value) {

    public static final int LENGTH = 6;
    private static final Pattern PATTERN = Pattern.compile("[A-Z0-9]{6}");

    public InviteCode {
        if (value == null || !PATTERN.matcher(value).matches()) {
            throw new InvalidValueException(
                    "The invite code must be 6 uppercase alphanumeric characters");
        }
    }

    /** Trims and uppercases the raw token before validating it. */
    public static InviteCode of(String raw) {
        if (raw == null) {
            return new InviteCode(null);
        }
        return new InviteCode(raw.trim().toUpperCase(Locale.ROOT));
    }
}
