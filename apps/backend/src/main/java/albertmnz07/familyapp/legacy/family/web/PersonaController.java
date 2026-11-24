package albertmnz07.familyapp.legacy.family.web;

import albertmnz07.familyapp.legacy.family.Persona;
import albertmnz07.familyapp.legacy.family.PersonaService;
import albertmnz07.familyapp.legacy.family.error.NotFoundException;
import albertmnz07.familyapp.legacy.family.photo.PhotoStore;
import albertmnz07.familyapp.legacy.family.photo.PhotoUpload;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/personas")
@Validated
@Tag(name = "Personas")
public class PersonaController {

    private final PersonaService personas;
    private final PhotoStore photos;

    public PersonaController(PersonaService personas, PhotoStore photos) {
        this.personas = personas;
        this.photos = photos;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PersonaResponse register(
            @RequestParam
                    @NotBlank
                    @Pattern(
                            regexp = "[A-Za-z0-9._-]{3,30}",
                            message =
                                    "must be 3-30 characters: letters, digits, dots, underscores or hyphens")
                    String username,
            @RequestParam @NotBlank String dni,
            @RequestParam @NotBlank @Size(min = 3, max = 100, message = "must be 3-100 characters")
                    String password) {
        return PersonaResponse.of(personas.register(username, dni, password));
    }

    @GetMapping("/me")
    public PersonaResponse me(@ActingPersona Persona persona) {
        return PersonaResponse.of(persona);
    }

    @PutMapping(value = "/me/dni/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PersonaResponse updateDniPhotos(
            @Parameter(hidden = true) @ActingPersona Persona persona,
            @RequestParam(required = false) MultipartFile frontPhoto,
            @RequestParam(required = false) MultipartFile backPhoto)
            throws IOException {
        return PersonaResponse.of(
                personas.updateDniPhotos(persona, uploadOf(frontPhoto), uploadOf(backPhoto)));
    }

    /** An omitted or empty part means "leave this side as it is". */
    private static PhotoUpload uploadOf(MultipartFile part) throws IOException {
        if (part == null || part.isEmpty()) {
            return null;
        }
        return new PhotoUpload(part.getBytes(), part.getContentType());
    }

    @GetMapping("/me/photos/front")
    public ResponseEntity<byte[]> myFrontPhoto(@ActingPersona Persona persona) {
        return photos.get(persona.getFrontPhotoKey())
                .map(photo -> PhotoResponses.of(photo, persona.getFrontPhotoContentType()))
                .orElseThrow(() -> new NotFoundException("Front photo not found"));
    }

    @GetMapping("/me/photos/back")
    public ResponseEntity<byte[]> myBackPhoto(@ActingPersona Persona persona) {
        return photos.get(persona.getBackPhotoKey())
                .map(photo -> PhotoResponses.of(photo, persona.getBackPhotoContentType()))
                .orElseThrow(() -> new NotFoundException("Back photo not found"));
    }
}
