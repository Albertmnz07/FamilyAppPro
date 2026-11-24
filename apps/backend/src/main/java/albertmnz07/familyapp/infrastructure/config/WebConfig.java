package albertmnz07.familyapp.infrastructure.config;

import albertmnz07.familyapp.infrastructure.web.ActingPersonaArgumentResolver;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ActingPersonaArgumentResolver actingPersonaArgumentResolver;
    private final List<String> allowedOrigins;

    public WebConfig(
            ActingPersonaArgumentResolver actingPersonaArgumentResolver,
            @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
                    String allowedOrigins) {
        this.actingPersonaArgumentResolver = actingPersonaArgumentResolver;
        this.allowedOrigins =
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .toList();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(actingPersonaArgumentResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
