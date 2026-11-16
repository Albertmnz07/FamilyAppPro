package albertmnz07.demo.family.web;

import albertmnz07.demo.family.Persona;
import albertmnz07.demo.family.PersonaService;
import albertmnz07.demo.family.validation.ValidDni;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api/v1/personas")
@Validated
@Tag(name = "Personas")
public class PersonaController {

    private final PersonaService personas;

    public PersonaController(PersonaService personas) {
        this.personas = personas;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PersonaResponse register(
            @RequestParam @NotBlank @Pattern(
                    regexp = "[A-Za-z0-9._-]{3,30}",
                    message = "must be 3-30 characters: letters, digits, dots, underscores or hyphens") String username,
            @RequestParam @NotBlank @ValidDni String dni,
            @RequestPart("frontPhoto") MultipartFile frontPhoto,
            @RequestPart("backPhoto") MultipartFile backPhoto) throws IOException {
        Persona persona = personas.register(username, dni, frontPhoto.getBytes(), frontPhoto.getContentType(),
                backPhoto.getBytes(), backPhoto.getContentType());
        return PersonaResponse.of(persona);
    }

    @GetMapping("/me")
    public PersonaResponse me(@ActingPersona Persona persona) {
        return PersonaResponse.of(persona);
    }

    @GetMapping("/me/photos/front")
    public ResponseEntity<byte[]> myFrontPhoto(@ActingPersona Persona persona) {
        return PhotoResponses.of(persona.getFrontPhoto(), persona.getFrontPhotoContentType());
    }

    @GetMapping("/me/photos/back")
    public ResponseEntity<byte[]> myBackPhoto(@ActingPersona Persona persona) {
        return PhotoResponses.of(persona.getBackPhoto(), persona.getBackPhotoContentType());
    }
}
