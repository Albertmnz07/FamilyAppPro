package albertmnz07.familyapp.legacy.family.web;

import albertmnz07.familyapp.legacy.family.Persona;
import albertmnz07.familyapp.legacy.family.auth.AuthService;
import albertmnz07.familyapp.legacy.family.error.UnauthorizedException;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class ActingPersonaArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService auth;

    public ActingPersonaArgumentResolver(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ActingPersona.class)
                && Persona.class.equals(parameter.getParameterType());
    }

    @Override
    public Persona resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
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
        return auth.resolve(token);
    }
}
