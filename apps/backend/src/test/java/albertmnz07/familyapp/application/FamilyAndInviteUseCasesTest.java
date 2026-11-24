package albertmnz07.familyapp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import albertmnz07.familyapp.application.command.CreateFamilyCommand;
import albertmnz07.familyapp.application.command.CreateFamilyCommandHandler;
import albertmnz07.familyapp.application.command.IssueInviteCommand;
import albertmnz07.familyapp.application.command.IssueInviteCommandHandler;
import albertmnz07.familyapp.application.command.IssueInviteResult;
import albertmnz07.familyapp.application.command.RedeemInviteCommand;
import albertmnz07.familyapp.application.command.RedeemInviteCommandHandler;
import albertmnz07.familyapp.application.command.RedeemInviteResult;
import albertmnz07.familyapp.application.port.Clock;
import albertmnz07.familyapp.application.port.InviteCodeGenerator;
import albertmnz07.familyapp.application.query.FamilyView;
import albertmnz07.familyapp.application.query.GetFamilyQuery;
import albertmnz07.familyapp.application.query.GetFamilyQueryHandler;
import albertmnz07.familyapp.application.query.ListMyFamiliesQuery;
import albertmnz07.familyapp.application.query.ListMyFamiliesQueryHandler;
import albertmnz07.familyapp.application.query.PageQuery;
import albertmnz07.familyapp.application.query.PagedResult;
import albertmnz07.familyapp.domain.exception.ForbiddenException;
import albertmnz07.familyapp.domain.exception.InviteAlreadyRedeemedException;
import albertmnz07.familyapp.domain.exception.InviteExpiredException;
import albertmnz07.familyapp.domain.exception.NotFoundException;
import albertmnz07.familyapp.domain.model.Family;
import albertmnz07.familyapp.domain.model.Invite;
import albertmnz07.familyapp.domain.model.Membership;
import albertmnz07.familyapp.domain.repository.FamilyRepository;
import albertmnz07.familyapp.domain.repository.InviteRepository;
import albertmnz07.familyapp.domain.repository.MembershipRepository;
import albertmnz07.familyapp.domain.valueobject.FamilyId;
import albertmnz07.familyapp.domain.valueobject.InviteCode;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FamilyAndInviteUseCasesTest {

    private final Map<FamilyId, Family> familyStore = new HashMap<>();
    private final Map<String, Membership> membershipStore = new HashMap<>();
    private final Map<InviteCode, Invite> inviteStore = new HashMap<>();
    private final Deque<String> queuedCodes = new ArrayDeque<>();
    private int sequence = 100000;
    private Instant now = Instant.parse("2030-01-01T00:00:00Z");

    private static String key(FamilyId familyId, PersonaId personaId) {
        return familyId.value() + ":" + personaId.value();
    }

    private final FamilyRepository families =
            new FamilyRepository() {
                @Override
                public Family save(Family family) {
                    familyStore.put(family.id(), family);
                    return family;
                }

                @Override
                public Optional<Family> findById(FamilyId id) {
                    return Optional.ofNullable(familyStore.get(id));
                }

                @Override
                public List<Family> findAllByIds(Collection<FamilyId> ids) {
                    return ids.stream()
                            .map(familyStore::get)
                            .filter(java.util.Objects::nonNull)
                            .toList();
                }
            };

    private final MembershipRepository memberships =
            new MembershipRepository() {
                @Override
                public Membership save(Membership membership) {
                    membershipStore.put(
                            key(membership.familyId(), membership.personaId()), membership);
                    return membership;
                }

                @Override
                public Optional<Membership> findByFamilyAndPersona(
                        FamilyId familyId, PersonaId personaId) {
                    return Optional.ofNullable(membershipStore.get(key(familyId, personaId)));
                }

                @Override
                public boolean existsAccepted(FamilyId familyId, PersonaId personaId) {
                    return findByFamilyAndPersona(familyId, personaId)
                            .filter(Membership::isAccepted)
                            .isPresent();
                }

                @Override
                public List<Membership> findAcceptedByPersona(PersonaId personaId) {
                    return membershipStore.values().stream()
                            .filter(m -> m.personaId().equals(personaId) && m.isAccepted())
                            .toList();
                }

                @Override
                public List<Membership> findAcceptedByFamily(FamilyId familyId) {
                    return membershipStore.values().stream()
                            .filter(m -> m.familyId().equals(familyId) && m.isAccepted())
                            .toList();
                }
            };

    private final InviteRepository invites =
            new InviteRepository() {
                @Override
                public Invite save(Invite invite) {
                    inviteStore.put(invite.code(), invite);
                    return invite;
                }

                @Override
                public boolean existsByCode(InviteCode code) {
                    return inviteStore.containsKey(code);
                }

                @Override
                public Optional<Invite> findByCodeForUpdate(InviteCode code) {
                    return Optional.ofNullable(inviteStore.get(code));
                }
            };

    private final InviteCodeGenerator codeGenerator =
            () -> {
                if (!queuedCodes.isEmpty()) {
                    return InviteCode.of(queuedCodes.removeFirst());
                }
                return InviteCode.of(Integer.toString(sequence++));
            };

    private final Clock clock = () -> now;

    private final CreateFamilyCommandHandler createFamily =
            new CreateFamilyCommandHandler(families, memberships, clock);
    private final GetFamilyQueryHandler getFamily =
            new GetFamilyQueryHandler(families, memberships);
    private final ListMyFamiliesQueryHandler listMyFamilies =
            new ListMyFamiliesQueryHandler(memberships, families);
    private final IssueInviteCommandHandler issueInvite =
            new IssueInviteCommandHandler(families, invites, codeGenerator, clock);
    private final RedeemInviteCommandHandler redeemInvite =
            new RedeemInviteCommandHandler(invites, memberships, clock);

    @Test
    void creatingAFamilyEstablishesAdminAndAcceptedMembershipWithConsentInForce() {
        UUID creatorId = UUID.randomUUID();

        FamilyView created =
                createFamily.handle(new CreateFamilyCommand(creatorId, "Familia García"));

        assertThat(created.name()).isEqualTo("Familia García");
        assertThat(created.adminPersonaId()).isEqualTo(creatorId);
        Membership membership =
                memberships
                        .findByFamilyAndPersona(
                                new FamilyId(created.id()), new PersonaId(creatorId))
                        .orElseThrow();
        assertThat(membership.isAccepted()).isTrue();
        assertThat(membership.isConsentInForce()).isTrue();
    }

    @Test
    void getFamilyAllowsAcceptedMembersForbidsOutsidersAndReturnsNotFoundWhenMissing() {
        UUID adminId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        FamilyView created =
                createFamily.handle(new CreateFamilyCommand(adminId, "Familia García"));

        assertThat(getFamily.handle(new GetFamilyQuery(adminId, created.id()))).isEqualTo(created);
        assertThatThrownBy(() -> getFamily.handle(new GetFamilyQuery(outsiderId, created.id())))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> getFamily.handle(new GetFamilyQuery(adminId, UUID.randomUUID())))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listMyFamiliesOrdersCaseInsensitivelyExcludesRemovedMembershipsAndClampsPageSize() {
        UUID personaId = UUID.randomUUID();
        FamilyView z = createFamily.handle(new CreateFamilyCommand(personaId, "zúñiga"));
        createFamily.handle(new CreateFamilyCommand(personaId, "Alfaro"));
        createFamily.handle(new CreateFamilyCommand(personaId, "bermejo"));

        Membership removed =
                memberships
                        .findByFamilyAndPersona(new FamilyId(z.id()), new PersonaId(personaId))
                        .orElseThrow();
        removed.remove(now.plusSeconds(10));
        memberships.save(removed);

        PagedResult<FamilyView> clamped =
                listMyFamilies.handle(new ListMyFamiliesQuery(personaId, PageQuery.of(0, 500)));

        assertThat(clamped.size()).isEqualTo(100);
        assertThat(clamped.totalElements()).isEqualTo(2);
        assertThat(clamped.content())
                .extracting(FamilyView::name)
                .containsExactly("Alfaro", "bermejo");

        PagedResult<FamilyView> secondPage =
                listMyFamilies.handle(new ListMyFamiliesQuery(personaId, PageQuery.of(1, 1)));
        assertThat(secondPage.content()).extracting(FamilyView::name).containsExactly("bermejo");
        assertThat(secondPage.totalPages()).isEqualTo(2);
    }

    @Test
    void issueInviteRequiresAdminRetriesCollidingCodesAndSetsSevenDayExpiry() {
        UUID adminId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        FamilyView family = createFamily.handle(new CreateFamilyCommand(adminId, "Familia García"));

        assertThatThrownBy(
                        () -> issueInvite.handle(new IssueInviteCommand(outsiderId, family.id())))
                .isInstanceOf(ForbiddenException.class);

        queuedCodes.add("ABC234");
        IssueInviteResult first = issueInvite.handle(new IssueInviteCommand(adminId, family.id()));
        assertThat(first.code()).isEqualTo("ABC234");
        assertThat(first.expiresAt()).isEqualTo(now.plus(Duration.ofDays(7)));

        queuedCodes.add("ABC234");
        queuedCodes.add("XYZ789");
        IssueInviteResult second = issueInvite.handle(new IssueInviteCommand(adminId, family.id()));
        assertThat(second.code()).isEqualTo("XYZ789");
    }

    @Test
    void redeemInviteNormalizesCodeGrantsAndReGrantsConsentAndMapsFailureReasons() {
        UUID adminId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        FamilyView family = createFamily.handle(new CreateFamilyCommand(adminId, "Familia García"));

        queuedCodes.add("AB23CD");
        issueInvite.handle(new IssueInviteCommand(adminId, family.id()));

        RedeemInviteResult redeemed =
                redeemInvite.handle(new RedeemInviteCommand(memberId, "  ab23cd "));
        assertThat(redeemed.familyId()).isEqualTo(family.id());
        assertThat(redeemed.personaId()).isEqualTo(memberId);
        assertThat(redeemed.status()).isEqualTo("ACCEPTED");

        assertThatThrownBy(() -> redeemInvite.handle(new RedeemInviteCommand(otherId, "AB23CD")))
                .isInstanceOf(InviteAlreadyRedeemedException.class);
        assertThatThrownBy(
                        () -> redeemInvite.handle(new RedeemInviteCommand(otherId, "not-valid!")))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> redeemInvite.handle(new RedeemInviteCommand(otherId, "ZZZZZZ")))
                .isInstanceOf(NotFoundException.class);

        Membership member =
                memberships
                        .findByFamilyAndPersona(new FamilyId(family.id()), new PersonaId(memberId))
                        .orElseThrow();
        member.revokeConsent(now.plusSeconds(5));
        memberships.save(member);
        assertThat(member.isConsentInForce()).isFalse();

        queuedCodes.add("RENEW2");
        issueInvite.handle(new IssueInviteCommand(adminId, family.id()));
        redeemInvite.handle(new RedeemInviteCommand(memberId, "RENEW2"));
        assertThat(
                        memberships
                                .findByFamilyAndPersona(
                                        new FamilyId(family.id()), new PersonaId(memberId))
                                .orElseThrow()
                                .isConsentInForce())
                .isTrue();

        queuedCodes.add("EXPIR2");
        issueInvite.handle(new IssueInviteCommand(adminId, family.id()));
        now = now.plus(Duration.ofDays(7));
        assertThatThrownBy(() -> redeemInvite.handle(new RedeemInviteCommand(otherId, "EXPIR2")))
                .isInstanceOf(InviteExpiredException.class);
    }
}
