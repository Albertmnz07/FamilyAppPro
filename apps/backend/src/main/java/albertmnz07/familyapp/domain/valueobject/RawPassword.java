package albertmnz07.familyapp.domain.valueobject;

import albertmnz07.familyapp.domain.exception.InvalidValueException;

/** A clear-text password as typed by the user; never stored, never printed. */
public record RawPassword(String value) {

    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 100;

    public RawPassword {
        if (value == null || value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new InvalidValueException(
                    "The password must be " + MIN_LENGTH + "-" + MAX_LENGTH + " characters");
        }
    }

    @Override
    public String toString() {
        return "RawPassword[****]";
    }
}
