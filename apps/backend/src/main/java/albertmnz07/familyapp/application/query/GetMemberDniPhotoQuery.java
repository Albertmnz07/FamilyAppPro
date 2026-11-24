package albertmnz07.familyapp.application.query;

import albertmnz07.familyapp.domain.valueobject.PhotoSide;
import java.util.UUID;

public record GetMemberDniPhotoQuery(
        UUID viewerPersonaId, UUID familyId, UUID targetPersonaId, PhotoSide side) {}
