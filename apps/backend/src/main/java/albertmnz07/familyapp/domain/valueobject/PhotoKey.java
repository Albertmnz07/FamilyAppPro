package albertmnz07.familyapp.domain.valueobject;

import albertmnz07.familyapp.domain.exception.InvalidValueException;

/** The key under which a stored photo can be retrieved from the photo store. */
public record PhotoKey(String value) {

    public PhotoKey {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException("The photo key must not be blank");
        }
    }
}
