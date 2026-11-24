package albertmnz07.familyapp.infrastructure.web;

import albertmnz07.familyapp.application.command.CommandHandler;
import albertmnz07.familyapp.application.command.RedeemInviteCommand;
import albertmnz07.familyapp.application.command.RedeemInviteResult;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Invites")
public class InviteController {

    private final CommandHandler<RedeemInviteCommand, RedeemInviteResult> redeemInvite;

    public InviteController(CommandHandler<RedeemInviteCommand, RedeemInviteResult> redeemInvite) {
        this.redeemInvite = redeemInvite;
    }

    @PostMapping("/api/v1/invites/{code}/redemption")
    public RedemptionResponse redeem(
            @ActingPersona PersonaId personaId, @PathVariable String code) {
        return RedemptionResponse.of(
                redeemInvite.handle(new RedeemInviteCommand(personaId.value(), code)));
    }
}
