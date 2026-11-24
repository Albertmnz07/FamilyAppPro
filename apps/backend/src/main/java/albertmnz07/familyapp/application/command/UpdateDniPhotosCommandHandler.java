package albertmnz07.familyapp.application.command;

import albertmnz07.familyapp.application.port.PhotoStore;
import albertmnz07.familyapp.application.query.PersonaView;
import albertmnz07.familyapp.domain.exception.NotFoundException;
import albertmnz07.familyapp.domain.model.Persona;
import albertmnz07.familyapp.domain.repository.PersonaRepository;
import albertmnz07.familyapp.domain.valueobject.DniPhoto;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import albertmnz07.familyapp.domain.valueobject.RawPhoto;

public class UpdateDniPhotosCommandHandler
        implements CommandHandler<UpdateDniPhotosCommand, PersonaView> {

    private final PersonaRepository personas;
    private final PhotoStore photos;

    public UpdateDniPhotosCommandHandler(PersonaRepository personas, PhotoStore photos) {
        this.personas = personas;
        this.photos = photos;
    }

    /**
     * Both sides are validated before either is stored, so a rejected side leaves the other
     * untouched; an omitted side keeps its current photo.
     */
    @Override
    public PersonaView handle(UpdateDniPhotosCommand command) {
        PersonaId id = new PersonaId(command.personaId());
        Persona persona =
                personas.findById(id)
                        .orElseThrow(
                                () -> new NotFoundException("Persona not found: " + id.value()));
        RawPhoto front = rawOf(command.front(), "front");
        RawPhoto back = rawOf(command.back(), "back");
        persona.changeDniPhotos(store(front), store(back));
        return PersonaView.of(personas.save(persona));
    }

    private static RawPhoto rawOf(PhotoUpload upload, String side) {
        return upload == null ? null : RawPhoto.of(side, upload.content(), upload.contentType());
    }

    private DniPhoto store(RawPhoto photo) {
        return photo == null ? null : new DniPhoto(photos.put(photo), photo.contentType());
    }
}
