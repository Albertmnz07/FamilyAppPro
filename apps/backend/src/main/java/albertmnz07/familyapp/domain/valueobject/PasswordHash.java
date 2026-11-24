package albertmnz07.familyapp.domain.valueobject;

import albertmnz07.familyapp.domain.exception.InvalidValueException;

/** An opaque, already-hashed password as kept by the persona. */
public record PasswordHash(String value) {

    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException("The password hash must not be blank");
        }
    }

    @Override
    public String toString() {
        return "PasswordHash[****]";
    }
}
