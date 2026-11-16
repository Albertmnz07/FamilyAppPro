package albertmnz07.demo.family.error;

public class DuplicateDniException extends RuntimeException {

    public DuplicateDniException(String message) {
        super(message);
    }
}
