package albertmnz07.familyapp.application.query;

import albertmnz07.familyapp.domain.exception.NotFoundException;
import albertmnz07.familyapp.domain.repository.PersonaRepository;
import albertmnz07.familyapp.domain.valueobject.PersonaId;

public class GetMyPersonaQueryHandler implements QueryHandler<GetMyPersonaQuery, PersonaView> {

    private final PersonaRepository personas;

    public GetMyPersonaQueryHandler(PersonaRepository personas) {
        this.personas = personas;
    }

    @Override
    public PersonaView handle(GetMyPersonaQuery query) {
        PersonaId id = new PersonaId(query.personaId());
        return personas.findById(id)
                .map(PersonaView::of)
                .orElseThrow(() -> new NotFoundException("Persona not found: " + id.value()));
    }
}
