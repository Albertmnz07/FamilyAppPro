package albertmnz07.demo.family.web;

import albertmnz07.demo.family.auth.AuthService;
import albertmnz07.demo.family.auth.AuthToken;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AuthToken token = auth.login(request.username(), request.password());
        return new LoginResponse(token.getToken(), token.getPersona().getId(), token.getPersona().getUsername());
    }
}
