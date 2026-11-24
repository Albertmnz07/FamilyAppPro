package albertmnz07.familyapp.application.query;

import albertmnz07.familyapp.application.port.PhotoStore;
import albertmnz07.familyapp.domain.exception.NotFoundException;
import albertmnz07.familyapp.domain.repository.FamilyRepository;
import albertmnz07.familyapp.domain.repository.MembershipRepository;
import albertmnz07.familyapp.domain.repository.PersonaRepository;
import albertmnz07.familyapp.domain.valueobject.Dni;
import albertmnz07.familyapp.domain.valueobject.DniPhoto;
import albertmnz07.familyapp.domain.valueobject.FamilyId;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import java.util.Optional;

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
        NotFoundException missing =
                new NotFoundException(query.side().label() + " photo not found");
        Optional<DniPhoto> photo =
                query.side() == GetMemberDniPhotoQuery.Side.FRONT
                        ? visibleDni.front()
                        : visibleDni.back();
        DniPhoto stored = photo.orElseThrow(() -> missing);
        byte[] content = photos.get(stored.key()).orElseThrow(() -> missing);
        return new DniPhotoView(content, stored.contentType().value());
    }
}
