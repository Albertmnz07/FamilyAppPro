package albertmnz07.familyapp.infrastructure.web;

import albertmnz07.familyapp.application.command.CommandHandler;
import albertmnz07.familyapp.application.command.LoginCommand;
import albertmnz07.familyapp.application.command.LoginResult;
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

    private final CommandHandler<LoginCommand, LoginResult> login;

    public AuthController(CommandHandler<LoginCommand, LoginResult> login) {
        this.login = login;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = login.handle(new LoginCommand(request.username(), request.password()));
        return new LoginResponse(result.token(), result.personaId(), result.username());
    }
}
