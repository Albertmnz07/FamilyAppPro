package albertmnz07.demo.family.web;

import albertmnz07.demo.family.Dni;
import java.util.UUID;

public record DniResponse(
        UUID personaId,
        String number,
        String frontPhotoUrl,
        String backPhotoUrl) {

    public static DniResponse of(UUID familyId, UUID personaId, Dni dni) {
        String base = "/api/v1/families/" + familyId + "/members/" + personaId + "/dni";
        return new DniResponse(personaId, dni.number(),
                dni.hasFrontPhoto() ? base + "/front" : null,
                dni.hasBackPhoto() ? base + "/back" : null);
    }
}
