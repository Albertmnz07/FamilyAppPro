package albertmnz07.familyapp.infrastructure.web;

import albertmnz07.familyapp.application.query.MemberDniView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record DniResponse(
        UUID personaId,
        String number,
        @Schema(nullable = true) String frontPhotoUrl,
        @Schema(nullable = true) String backPhotoUrl) {

    public static DniResponse of(MemberDniView dni) {
        String base = "/api/v1/families/" + dni.familyId() + "/members/" + dni.personaId() + "/dni";
        return new DniResponse(
                dni.personaId(),
                dni.number(),
                dni.hasFrontPhoto() ? base + "/front" : null,
                dni.hasBackPhoto() ? base + "/back" : null);
    }
}
