package albertmnz07.familyapp.domain.valueobject;

import albertmnz07.familyapp.domain.exception.InvalidValueException;
import java.util.Locale;

/** The number identifying a persona: trimmed and uppercased, with no national format enforced. */
public record DniNumber(String value) {

    private static final int MAX_LENGTH = 255;

    public DniNumber {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            throw new InvalidValueException("The DNI must be 1-" + MAX_LENGTH + " characters");
        }
    }

    public static DniNumber of(String raw) {
        if (raw == null) {
            return new DniNumber(null);
        }
        return new DniNumber(raw.trim().toUpperCase(Locale.ROOT));
    }
}
