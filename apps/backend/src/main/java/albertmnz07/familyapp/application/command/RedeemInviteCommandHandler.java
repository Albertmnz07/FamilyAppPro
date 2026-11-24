package albertmnz07.familyapp.application.command;

import albertmnz07.familyapp.application.port.Clock;
import albertmnz07.familyapp.domain.exception.InvalidValueException;
import albertmnz07.familyapp.domain.exception.ResourceNotFoundException;
import albertmnz07.familyapp.domain.model.Invite;
import albertmnz07.familyapp.domain.model.Membership;
import albertmnz07.familyapp.domain.repository.InviteRepository;
import albertmnz07.familyapp.domain.repository.MembershipRepository;
import albertmnz07.familyapp.domain.valueobject.InviteCode;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import java.time.Instant;

public class RedeemInviteCommandHandler
        implements CommandHandler<RedeemInviteCommand, RedeemInviteResult> {

    private final InviteRepository invites;
    private final MembershipRepository memberships;
    private final Clock clock;

    public RedeemInviteCommandHandler(
            InviteRepository invites, MembershipRepository memberships, Clock clock) {
        this.invites = invites;
        this.memberships = memberships;
        this.clock = clock;
    }

    @Override
    public RedeemInviteResult handle(RedeemInviteCommand command) {
        PersonaId personaId = new PersonaId(command.personaId());
        InviteCode code = parseInviteCode(command.code());
        Invite invite =
                invites.findByCodeForUpdate(code)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Invite not found: " + command.code()));
        Instant now = clock.now();
        invite.redeem(now);
        invites.save(invite);
        Membership membership =
                memberships
                        .findByFamilyAndPersona(invite.familyId(), personaId)
                        .map(existing -> {
                            existing.accept(now);
                            return existing;
                        })
                        .orElseGet(() -> Membership.acceptNew(invite.familyId(), personaId, now));
        Membership saved = memberships.save(membership);
        return new RedeemInviteResult(
                saved.familyId().value(), saved.personaId().value(), saved.status().name());
    }

    private static InviteCode parseInviteCode(String raw) {
        try {
            return InviteCode.of(raw);
        } catch (InvalidValueException malformed) {
            throw new ResourceNotFoundException("Invite not found: " + raw);
        }
    }
}
