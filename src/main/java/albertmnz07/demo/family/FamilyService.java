package albertmnz07.demo.family;

import albertmnz07.demo.family.error.CannotRemoveAdminException;
import albertmnz07.demo.family.error.ForbiddenException;
import albertmnz07.demo.family.error.InviteAlreadyRedeemedException;
import albertmnz07.demo.family.error.InviteExpiredException;
import albertmnz07.demo.family.error.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class FamilyService {

    static final Duration INVITE_TTL = Duration.ofDays(7);
    private static final int MAX_CODE_ATTEMPTS = 10;
    public static final int MAX_PAGE_SIZE = 100;

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
        Membership membership = inviteMembership(family, creator);
        acceptMembership(membership, Instant.now());
        return family;
    }

    public Family getFamily(UUID id) {
        return families.findById(id)
                .orElseThrow(() -> new NotFoundException("Family not found: " + id));
    }

    public Page<Family> listFamiliesFor(Persona persona, Pageable pageable) {
        return page(memberships.findByPersonaAndStatus(persona, Membership.Status.ACCEPTED).stream()
                .map(Membership::getFamily)
                .sorted(Comparator.comparing(Family::getName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList(), pageable);
    }

    public List<Membership> listAcceptedMembers(Family family) {
        return memberships.findByFamilyAndStatus(family, Membership.Status.ACCEPTED).stream()
                .sorted(Comparator.comparing(membership -> membership.getPersona().getDni(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Page<Membership> listAcceptedMembers(Family family, Pageable pageable) {
        return page(listAcceptedMembers(family), pageable);
    }

    private static <T> Page<T> page(List<T> sorted, Pageable pageable) {
        // Ordering is fixed server-side and the page size is capped, so rebind the
        // pageable (unsorted, bounded) instead of echoing the client's request.
        Pageable bounded = PageRequest.of(pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        int start = (int) Math.min(bounded.getOffset(), sorted.size());
        int end = Math.min(start + bounded.getPageSize(), sorted.size());
        return new PageImpl<>(sorted.subList(start, end), bounded, sorted.size());
    }

    public boolean isAcceptedMember(Family family, Persona persona) {
        return memberships.existsByFamilyAndPersonaAndStatus(family, persona, Membership.Status.ACCEPTED);
    }

    public boolean isConsentInForce(Family family, Persona persona) {
        return consents.findByFamilyAndPersona(family, persona)
                .map(Consent::isInForce)
                .orElse(false);
    }

    public void requireAcceptedMember(Family family, Persona persona) {
        if (!isAcceptedMember(family, persona)) {
            throw new ForbiddenException("Persona is not a member of this family");
        }
    }

    public void requireAdmin(Family family, Persona persona) {
        if (!family.getAdmin().getId().equals(persona.getId())) {
            throw new ForbiddenException("Only the family admin can perform this action");
        }
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
        Invite invite = invites.findByCode(normalizeCode(code))
                .orElseThrow(() -> new NotFoundException("Invite not found: " + code));
        Instant now = Instant.now();
        if (invite.isExpiredAt(now)) {
            throw new InviteExpiredException("Invite has expired: " + code);
        }
        if (invite.isRedeemed()) {
            throw new InviteAlreadyRedeemedException("Invite has already been redeemed: " + code);
        }
        invite.redeem(now);
        invites.save(invite);
        Membership membership = inviteMembership(invite.getFamily(), persona);
        return acceptMembership(membership, now);
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    private Membership inviteMembership(Family family, Persona persona) {
        return memberships.findByFamilyAndPersona(family, persona)
                .orElseGet(() -> memberships.save(new Membership(family, persona)));
    }

    private Membership acceptMembership(Membership membership, Instant now) {
        membership.accept();
        Membership accepted = memberships.save(membership);
        grantConsent(membership.getFamily(), membership.getPersona(), now);
        return accepted;
    }

    private void grantConsent(Family family, Persona persona, Instant now) {
        Consent consent = consents.findByFamilyAndPersona(family, persona)
                .orElseGet(() -> new Consent(family, persona, now));
        consent.grant(now);
        consents.save(consent);
    }

    @Transactional
    public void removeMember(Family family, Persona persona) {
        if (family.getAdmin().getId().equals(persona.getId())) {
            throw new CannotRemoveAdminException("Cannot remove the family admin");
        }
        memberships.findByFamilyAndPersona(family, persona)
                .ifPresent(membership -> {
                    membership.remove();
                    memberships.save(membership);
                });
        revokeConsent(family, persona);
    }

    @Transactional
    public void revokeConsent(Family family, Persona persona) {
        consents.findByFamilyAndPersona(family, persona)
                .filter(Consent::isInForce)
                .ifPresent(consent -> {
                    consent.revoke(Instant.now());
                    consents.save(consent);
                });
    }

    public Optional<Dni> visibleDni(Family family, Persona viewer, Persona target) {
        if (!isAcceptedMember(family, viewer)) {
            return Optional.empty();
        }
        if (!isConsentInForce(family, target)) {
            return Optional.empty();
        }
        return Optional.of(target.dniEntity());
    }
}
