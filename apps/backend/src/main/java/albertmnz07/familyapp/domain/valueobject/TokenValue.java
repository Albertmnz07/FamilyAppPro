package albertmnz07.familyapp.domain.valueobject;

import albertmnz07.familyapp.domain.exception.InvalidValueException;
import java.util.UUID;

/** The opaque bearer-token string handed to the client (ADR-0004). */
public record TokenValue(String value) {

    public TokenValue {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException("The token must not be blank");
        }
    }

    public static TokenValue generate() {
        return new TokenValue(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return "TokenValue[****]";
    }
}
