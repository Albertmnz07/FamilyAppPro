package albertmnz07.familyapp.application.command;

/** A state-changing use case. */
public interface CommandHandler<C, R> {

    R handle(C command);
}
