package albertmnz07.familyapp.domain.valueobject;

import albertmnz07.familyapp.domain.exception.InvalidValueException;

/** The display name of a family: non-blank and up to 100 characters. */
public record FamilyName(String value) {

    public static final int MAX_LENGTH = 100;

    public FamilyName {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            throw new InvalidValueException(
                    "The family name must be 1-" + MAX_LENGTH + " characters");
        }
    }

    public static FamilyName of(String raw) {
        return new FamilyName(raw);
    }
}
