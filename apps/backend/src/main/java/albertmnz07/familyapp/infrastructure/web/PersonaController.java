package albertmnz07.familyapp.infrastructure.web;

import albertmnz07.familyapp.application.command.CommandHandler;
import albertmnz07.familyapp.application.command.PhotoUpload;
import albertmnz07.familyapp.application.command.RegisterPersonaCommand;
import albertmnz07.familyapp.application.command.UpdateDniPhotosCommand;
import albertmnz07.familyapp.application.query.DniPhotoView;
import albertmnz07.familyapp.application.query.GetMyDniPhotoQuery;
import albertmnz07.familyapp.application.query.GetMyDniPhotoQuery.Side;
import albertmnz07.familyapp.application.query.GetMyPersonaQuery;
import albertmnz07.familyapp.application.query.PersonaView;
import albertmnz07.familyapp.application.query.QueryHandler;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
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

    private final CommandHandler<RegisterPersonaCommand, PersonaView> register;
    private final CommandHandler<UpdateDniPhotosCommand, PersonaView> updateDniPhotos;
    private final QueryHandler<GetMyPersonaQuery, PersonaView> getMyPersona;
    private final QueryHandler<GetMyDniPhotoQuery, DniPhotoView> getMyDniPhoto;

    public PersonaController(
            CommandHandler<RegisterPersonaCommand, PersonaView> register,
            CommandHandler<UpdateDniPhotosCommand, PersonaView> updateDniPhotos,
            QueryHandler<GetMyPersonaQuery, PersonaView> getMyPersona,
            QueryHandler<GetMyDniPhotoQuery, DniPhotoView> getMyDniPhoto) {
        this.register = register;
        this.updateDniPhotos = updateDniPhotos;
        this.getMyPersona = getMyPersona;
        this.getMyDniPhoto = getMyDniPhoto;
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
        return PersonaResponse.of(
                register.handle(new RegisterPersonaCommand(username, dni, password)));
    }

    @GetMapping("/me")
    public PersonaResponse me(@Parameter(hidden = true) @ActingPersona PersonaId personaId) {
        return PersonaResponse.of(getMyPersona.handle(new GetMyPersonaQuery(personaId.value())));
    }

    @PutMapping(value = "/me/dni/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PersonaResponse updateDniPhotos(
            @Parameter(hidden = true) @ActingPersona PersonaId personaId,
            @RequestParam(required = false) MultipartFile frontPhoto,
            @RequestParam(required = false) MultipartFile backPhoto)
            throws IOException {
        return PersonaResponse.of(
                updateDniPhotos.handle(
                        new UpdateDniPhotosCommand(
                                personaId.value(), uploadOf(frontPhoto), uploadOf(backPhoto))));
    }

    /** An omitted or empty part means "leave this side as it is". */
    private static PhotoUpload uploadOf(MultipartFile part) throws IOException {
        if (part == null || part.isEmpty()) {
            return null;
        }
        return new PhotoUpload(part.getBytes(), part.getContentType());
    }

    @GetMapping("/me/photos/front")
    public ResponseEntity<byte[]> myFrontPhoto(
            @Parameter(hidden = true) @ActingPersona PersonaId personaId) {
        return photo(personaId, Side.FRONT);
    }

    @GetMapping("/me/photos/back")
    public ResponseEntity<byte[]> myBackPhoto(
            @Parameter(hidden = true) @ActingPersona PersonaId personaId) {
        return photo(personaId, Side.BACK);
    }

    private ResponseEntity<byte[]> photo(PersonaId personaId, Side side) {
        DniPhotoView photo = getMyDniPhoto.handle(new GetMyDniPhotoQuery(personaId.value(), side));
        return PhotoResponses.of(photo.content(), photo.contentType());
    }
}
