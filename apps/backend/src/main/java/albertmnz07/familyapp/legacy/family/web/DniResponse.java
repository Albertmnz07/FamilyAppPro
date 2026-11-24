package albertmnz07.familyapp.legacy.family.web;

import albertmnz07.familyapp.legacy.family.Dni;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record DniResponse(
        UUID personaId,
        String number,
        @Schema(nullable = true) String frontPhotoUrl,
        @Schema(nullable = true) String backPhotoUrl) {

    public static DniResponse of(UUID familyId, UUID personaId, Dni dni) {
        String base = "/api/v1/families/" + familyId + "/members/" + personaId + "/dni";
        return new DniResponse(
                personaId,
                dni.number(),
                dni.hasFrontPhoto() ? base + "/front" : null,
                dni.hasBackPhoto() ? base + "/back" : null);
    }
}
