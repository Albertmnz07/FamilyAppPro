package albertmnz07.familyapp.application.query;

import albertmnz07.familyapp.domain.valueobject.PhotoSide;
import java.util.UUID;

public record GetMyDniPhotoQuery(UUID personaId, PhotoSide side) {}
