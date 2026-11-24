package albertmnz07.familyapp.domain.exception;

/** Root of every business or value refusal raised by the domain and application layers. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
