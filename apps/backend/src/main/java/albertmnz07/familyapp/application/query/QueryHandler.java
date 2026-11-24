package albertmnz07.familyapp.application.query;

/** A read-only use case. */
public interface QueryHandler<Q, R> {

    R handle(Q query);
}
