package albertmnz07.familyapp.application.query;

import albertmnz07.familyapp.application.port.Clock;
import albertmnz07.familyapp.domain.exception.InvalidValueException;
import albertmnz07.familyapp.domain.exception.UnauthorizedException;
import albertmnz07.familyapp.domain.model.AuthToken;
import albertmnz07.familyapp.domain.repository.AuthTokenRepository;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import albertmnz07.familyapp.domain.valueobject.TokenValue;

/** Resolves a bearer token to the persona it was issued to. */
public class AuthenticateTokenQueryHandler
        implements QueryHandler<AuthenticateTokenQuery, PersonaId> {

    private final AuthTokenRepository tokens;
    private final Clock clock;

    public AuthenticateTokenQueryHandler(AuthTokenRepository tokens, Clock clock) {
        this.tokens = tokens;
        this.clock = clock;
    }

    @Override
    public PersonaId handle(AuthenticateTokenQuery query) {
        TokenValue value;
        try {
            value = new TokenValue(query.token());
        } catch (InvalidValueException blank) {
            throw new UnauthorizedException("Unknown token");
        }
        AuthToken token =
                tokens.findByValue(value)
                        .orElseThrow(() -> new UnauthorizedException("Unknown token"));
        token.requireActiveAt(clock.now());
        return token.personaId();
    }
}
