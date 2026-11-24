package albertmnz07.familyapp.application.command;

import albertmnz07.familyapp.application.port.Clock;
import albertmnz07.familyapp.application.port.InviteCodeGenerator;
import albertmnz07.familyapp.domain.exception.NotFoundException;
import albertmnz07.familyapp.domain.model.Family;
import albertmnz07.familyapp.domain.model.Invite;
import albertmnz07.familyapp.domain.repository.FamilyRepository;
import albertmnz07.familyapp.domain.repository.InviteRepository;
import albertmnz07.familyapp.domain.valueobject.FamilyId;
import albertmnz07.familyapp.domain.valueobject.InviteCode;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import java.time.Duration;
import java.time.Instant;

public class IssueInviteCommandHandler
        implements CommandHandler<IssueInviteCommand, IssueInviteResult> {

    static final Duration INVITE_TTL = Duration.ofDays(7);
    private static final int MAX_CODE_ATTEMPTS = 10;

    private final FamilyRepository families;
    private final InviteRepository invites;
    private final InviteCodeGenerator codeGenerator;
    private final Clock clock;

    public IssueInviteCommandHandler(
            FamilyRepository families,
            InviteRepository invites,
            InviteCodeGenerator codeGenerator,
            Clock clock) {
        this.families = families;
        this.invites = invites;
        this.codeGenerator = codeGenerator;
        this.clock = clock;
    }

    @Override
    public IssueInviteResult handle(IssueInviteCommand command) {
        FamilyId familyId = new FamilyId(command.familyId());
        PersonaId actorId = new PersonaId(command.actorPersonaId());
        Family family =
                families.findById(familyId)
                        .orElseThrow(() ->
                                new NotFoundException("Family not found: " + familyId.value()));
        family.requireAdmin(actorId);
        Instant issuedAt = clock.now();
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            InviteCode code = codeGenerator.generate();
            if (!invites.existsByCode(code)) {
                Invite saved = invites.save(Invite.issue(code, family.id(), issuedAt, INVITE_TTL));
                return new IssueInviteResult(
                        saved.code().value(), saved.familyId().value(), saved.expiresAt());
            }
        }
        throw new IllegalStateException("Could not generate a unique invite code");
    }
}
