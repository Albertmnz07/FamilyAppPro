package albertmnz07.demo.family.error;

public class CannotRemoveAdminException extends IllegalArgumentException {

    public CannotRemoveAdminException(String message) {
        super(message);
    }
}
