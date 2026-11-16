package albertmnz07.demo.family.web;

import albertmnz07.demo.family.Persona;
import albertmnz07.demo.family.PersonaRepository;
import albertmnz07.demo.family.error.UnauthorizedException;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import java.util.UUID;

@Component
public class ActingPersonaArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String HEADER = "X-Persona-Id";

    private final PersonaRepository personas;

    public ActingPersonaArgumentResolver(PersonaRepository personas) {
        this.personas = personas;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ActingPersona.class)
                && Persona.class.equals(parameter.getParameterType());
    }

    @Override
    public Persona resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String header = webRequest.getHeader(HEADER);
        if (header == null || header.isBlank()) {
            throw new UnauthorizedException("Missing " + HEADER + " header");
        }
        UUID personaId;
        try {
            personaId = UUID.fromString(header.trim());
        } catch (IllegalArgumentException invalidUuid) {
            throw new UnauthorizedException("Invalid " + HEADER + " header");
        }
        return personas.findById(personaId)
                .orElseThrow(() -> new UnauthorizedException("Unknown persona: " + personaId));
    }
}
