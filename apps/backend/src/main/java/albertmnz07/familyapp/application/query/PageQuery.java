package albertmnz07.familyapp.application.query;

import albertmnz07.familyapp.domain.exception.InvalidValueException;

/** Server-bounded pagination input (default size 20, clamped to 100, client sort ignored). */
public record PageQuery(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new InvalidValueException("Page index must not be negative");
        }
        if (size < 1) {
            throw new InvalidValueException("Page size must be at least 1");
        }
        size = Math.min(size, MAX_SIZE);
    }

    public static PageQuery of(int page, int size) {
        return new PageQuery(Math.max(0, page), size < 1 ? DEFAULT_SIZE : size);
    }
}
