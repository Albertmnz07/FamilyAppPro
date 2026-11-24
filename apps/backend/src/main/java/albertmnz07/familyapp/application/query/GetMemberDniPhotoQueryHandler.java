package albertmnz07.familyapp.application.query;

import albertmnz07.familyapp.application.port.PhotoStore;
import albertmnz07.familyapp.domain.exception.ResourceNotFoundException;
import albertmnz07.familyapp.domain.repository.FamilyRepository;
import albertmnz07.familyapp.domain.repository.MembershipRepository;
import albertmnz07.familyapp.domain.repository.PersonaRepository;
import albertmnz07.familyapp.domain.valueobject.Dni;
import albertmnz07.familyapp.domain.valueobject.DniPhoto;
import albertmnz07.familyapp.domain.valueobject.FamilyId;
import albertmnz07.familyapp.domain.valueobject.PersonaId;

public class GetMemberDniPhotoQueryHandler
        implements QueryHandler<GetMemberDniPhotoQuery, DniPhotoView> {

    private final FamilyRepository families;
    private final MembershipRepository memberships;
    private final PersonaRepository personas;
    private final PhotoStore photos;

    public GetMemberDniPhotoQueryHandler(
            FamilyRepository families,
            MembershipRepository memberships,
            PersonaRepository personas,
            PhotoStore photos) {
        this.families = families;
        this.memberships = memberships;
        this.personas = personas;
        this.photos = photos;
    }

    @Override
    public DniPhotoView handle(GetMemberDniPhotoQuery query) {
        FamilyId familyId = new FamilyId(query.familyId());
        PersonaId viewerId = new PersonaId(query.viewerPersonaId());
        PersonaId targetId = new PersonaId(query.targetPersonaId());
        Dni visibleDni =
                MemberDniVisibility.requireVisibleDni(
                        families, memberships, personas, familyId, viewerId, targetId);
        ResourceNotFoundException missing =
                new ResourceNotFoundException(query.side().displayName() + " photo not found");
        DniPhoto stored = visibleDni.photo(query.side()).orElseThrow(() -> missing);
        byte[] content = photos.get(stored.key()).orElseThrow(() -> missing);
        return new DniPhotoView(content, stored.contentType().value());
    }
}
