package albertmnz07.demo.family.error;

public class PhotoTooLargeException extends RuntimeException {

    public PhotoTooLargeException(String message) {
        super(message);
    }
}
