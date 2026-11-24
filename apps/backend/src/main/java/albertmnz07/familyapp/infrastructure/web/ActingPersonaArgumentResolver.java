package albertmnz07.familyapp.infrastructure.web;

import albertmnz07.familyapp.application.query.AuthenticateTokenQuery;
import albertmnz07.familyapp.application.query.QueryHandler;
import albertmnz07.familyapp.domain.exception.UnauthorizedException;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import albertmnz07.familyapp.legacy.family.Persona;
import albertmnz07.familyapp.legacy.family.PersonaRepository;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@code @ActingPersona} parameters from the bearer token. A {@link PersonaId} parameter
 * is the migrated form; the legacy {@link Persona} entity is still supported transitionally for
 * controllers not yet migrated.
 */
@Component
public class ActingPersonaArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final QueryHandler<AuthenticateTokenQuery, PersonaId> authenticate;
    private final PersonaRepository legacyPersonas;

    public ActingPersonaArgumentResolver(
            QueryHandler<AuthenticateTokenQuery, PersonaId> authenticate,
            PersonaRepository legacyPersonas) {
        this.authenticate = authenticate;
        this.legacyPersonas = legacyPersonas;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ActingPersona.class)
                && (PersonaId.class.equals(parameter.getParameterType())
                        || Persona.class.equals(parameter.getParameterType()));
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        PersonaId id = authenticate.handle(new AuthenticateTokenQuery(bearerToken(webRequest)));
        if (PersonaId.class.equals(parameter.getParameterType())) {
            return id;
        }
        return legacyPersonas
                .findById(id.value())
                .orElseThrow(() -> new UnauthorizedException("Unknown token"));
    }

    private static String bearerToken(NativeWebRequest webRequest) {
        String header = webRequest.getHeader(HEADER);
        if (header == null || header.isBlank()) {
            throw new UnauthorizedException("Missing " + HEADER + " header");
        }
        String token = header.trim();
        if (!token.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new UnauthorizedException("Invalid " + HEADER + " header");
        }
        token = token.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new UnauthorizedException("Invalid " + HEADER + " header");
        }
        return token;
    }
}
