package albertmnz07.familyapp.application.query;

import albertmnz07.familyapp.application.port.PhotoStore;
import albertmnz07.familyapp.domain.exception.ResourceNotFoundException;
import albertmnz07.familyapp.domain.model.Persona;
import albertmnz07.familyapp.domain.repository.PersonaRepository;
import albertmnz07.familyapp.domain.valueobject.DniPhoto;
import albertmnz07.familyapp.domain.valueobject.PersonaId;

public class GetMyDniPhotoQueryHandler implements QueryHandler<GetMyDniPhotoQuery, DniPhotoView> {

    private final PersonaRepository personas;
    private final PhotoStore photos;

    public GetMyDniPhotoQueryHandler(PersonaRepository personas, PhotoStore photos) {
        this.personas = personas;
        this.photos = photos;
    }

    @Override
    public DniPhotoView handle(GetMyDniPhotoQuery query) {
        PersonaId id = new PersonaId(query.personaId());
        Persona persona =
                personas.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Persona not found: " + id.value()));
        ResourceNotFoundException missing =
                new ResourceNotFoundException(query.side().displayName() + " photo not found");
        DniPhoto stored = persona.dni().photo(query.side()).orElseThrow(() -> missing);
        byte[] content = photos.get(stored.key()).orElseThrow(() -> missing);
        return new DniPhotoView(content, stored.contentType().value());
    }
}
