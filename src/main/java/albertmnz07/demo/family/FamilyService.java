package albertmnz07.demo.family;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class FamilyService {

    static final Duration INVITE_TTL = Duration.ofDays(7);
    private static final int MAX_CODE_ATTEMPTS = 10;

    private final FamilyRepository families;
    private final InviteRepository invites;
    private final MembershipRepository memberships;
    private final ConsentRepository consents;
    private final InviteCodeGenerator codeGenerator;

    public FamilyService(FamilyRepository families, InviteRepository invites, MembershipRepository memberships,
            ConsentRepository consents, InviteCodeGenerator codeGenerator) {
        this.families = families;
        this.invites = invites;
        this.memberships = memberships;
        this.consents = consents;
        this.codeGenerator = codeGenerator;
    }

    @Transactional
    public Family createFamily(Persona creator, String name) {
        Family family = families.save(new Family(name, creator));
        acceptMembership(family, creator, Instant.now());
        return family;
    }

    public Family getFamily(UUID id) {
        return families.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Family not found: " + id));
    }

    public Invite issueInvite(Family family) {
        Instant issuedAt = Instant.now();
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = codeGenerator.generate();
            if (!invites.existsByCode(code)) {
                return invites.save(new Invite(code, family, issuedAt.plus(INVITE_TTL)));
            }
        }
        throw new IllegalStateException("Could not generate a unique invite code");
    }

    @Transactional
    public Membership redeemInvite(String code, Persona persona) {
        Invite invite = invites.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException("Invite not found: " + code));
        Instant now = Instant.now();
        if (invite.isExpiredAt(now)) {
            throw new IllegalStateException("Invite has expired: " + code);
        }
        if (invite.isRedeemed()) {
            throw new IllegalStateException("Invite has already been redeemed: " + code);
        }
        invite.redeem(now);
        invites.save(invite);
        return acceptMembership(invite.getFamily(), persona, now);
    }

    private Membership acceptMembership(Family family, Persona persona, Instant now) {
        Membership membership = new Membership(family, persona);
        membership.accept();
        Membership accepted = memberships.save(membership);
        consents.save(new Consent(family, persona, now));
        return accepted;
    }

    public Optional<Dni> visibleDni(Family family, Persona viewer, Persona target) {
        boolean viewerIsMember = memberships.existsByFamilyAndPersonaAndStatus(
                family, viewer, Membership.Status.ACCEPTED);
        if (!viewerIsMember) {
            return Optional.empty();
        }
        boolean consentInForce = consents.existsByFamilyAndPersona(family, target);
        if (!consentInForce) {
            return Optional.empty();
        }
        return Optional.of(target.dniEntity());
    }
}
