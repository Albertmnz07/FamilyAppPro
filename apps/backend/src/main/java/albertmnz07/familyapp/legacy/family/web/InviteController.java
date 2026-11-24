package albertmnz07.familyapp.legacy.family.web;

import albertmnz07.familyapp.legacy.family.FamilyService;
import albertmnz07.familyapp.legacy.family.Membership;
import albertmnz07.familyapp.legacy.family.Persona;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Invites")
public class InviteController {

    private final FamilyService families;

    public InviteController(FamilyService families) {
        this.families = families;
    }

    @PostMapping("/api/v1/invites/{code}/redemption")
    public RedemptionResponse redeem(@ActingPersona Persona persona, @PathVariable String code) {
        Membership membership = families.redeemInvite(code, persona);
        return RedemptionResponse.of(membership);
    }
}
