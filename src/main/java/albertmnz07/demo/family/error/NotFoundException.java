package albertmnz07.demo.family.error;

import java.util.NoSuchElementException;

public class NotFoundException extends NoSuchElementException {

    public NotFoundException(String message) {
        super(message);
    }
}
