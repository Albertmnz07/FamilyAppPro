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
import albertmnz07.familyapp.application.command.RemoveMemberCommand;
import albertmnz07.familyapp.application.command.RemoveMemberCommandHandler;
import albertmnz07.familyapp.application.command.RevokeConsentCommand;
import albertmnz07.familyapp.application.command.RevokeConsentCommandHandler;
import albertmnz07.familyapp.application.port.Clock;
import albertmnz07.familyapp.application.port.InviteCodeGenerator;
import albertmnz07.familyapp.application.port.PhotoStore;
import albertmnz07.familyapp.application.query.DniPhotoView;
import albertmnz07.familyapp.application.query.FamilyView;
import albertmnz07.familyapp.application.query.GetFamilyQuery;
import albertmnz07.familyapp.application.query.GetFamilyQueryHandler;
import albertmnz07.familyapp.application.query.GetMemberDniPhotoQuery;
import albertmnz07.familyapp.application.query.GetMemberDniPhotoQuery.Side;
import albertmnz07.familyapp.application.query.GetMemberDniPhotoQueryHandler;
import albertmnz07.familyapp.application.query.GetMemberDniQuery;
import albertmnz07.familyapp.application.query.GetMemberDniQueryHandler;
import albertmnz07.familyapp.application.query.ListFamilyMembersQuery;
import albertmnz07.familyapp.application.query.ListFamilyMembersQueryHandler;
import albertmnz07.familyapp.application.query.ListMyFamiliesQuery;
import albertmnz07.familyapp.application.query.ListMyFamiliesQueryHandler;
import albertmnz07.familyapp.application.query.MemberDniView;
import albertmnz07.familyapp.application.query.MemberView;
import albertmnz07.familyapp.application.query.PageQuery;
import albertmnz07.familyapp.application.query.PagedResult;
import albertmnz07.familyapp.domain.exception.CannotRemoveAdminException;
import albertmnz07.familyapp.domain.exception.ForbiddenException;
import albertmnz07.familyapp.domain.exception.InviteAlreadyRedeemedException;
import albertmnz07.familyapp.domain.exception.InviteExpiredException;
import albertmnz07.familyapp.domain.exception.NotFoundException;
import albertmnz07.familyapp.domain.model.Family;
import albertmnz07.familyapp.domain.model.Invite;
import albertmnz07.familyapp.domain.model.Membership;
import albertmnz07.familyapp.domain.model.Persona;
import albertmnz07.familyapp.domain.repository.FamilyRepository;
import albertmnz07.familyapp.domain.repository.InviteRepository;
import albertmnz07.familyapp.domain.repository.MembershipRepository;
import albertmnz07.familyapp.domain.repository.PersonaRepository;
import albertmnz07.familyapp.domain.valueobject.DniNumber;
import albertmnz07.familyapp.domain.valueobject.DniPhoto;
import albertmnz07.familyapp.domain.valueobject.FamilyId;
import albertmnz07.familyapp.domain.valueobject.InviteCode;
import albertmnz07.familyapp.domain.valueobject.PasswordHash;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import albertmnz07.familyapp.domain.valueobject.PhotoKey;
import albertmnz07.familyapp.domain.valueobject.RawPhoto;
import albertmnz07.familyapp.domain.valueobject.Username;
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

    private final Map<PersonaId, Persona> personaStore = new HashMap<>();
    private final Map<FamilyId, Family> familyStore = new HashMap<>();
    private final Map<String, Membership> membershipStore = new HashMap<>();
    private final Map<InviteCode, Invite> inviteStore = new HashMap<>();
    private final Map<PhotoKey, byte[]> photoBytes = new HashMap<>();
    private final Deque<String> queuedCodes = new ArrayDeque<>();
    private int sequence = 100000;
    private Instant now = Instant.parse("2030-01-01T00:00:00Z");

    private static String key(FamilyId familyId, PersonaId personaId) {
        return familyId.value() + ":" + personaId.value();
    }

    private final PersonaRepository personas =
            new PersonaRepository() {
                @Override
                public Persona save(Persona persona) {
                    personaStore.put(persona.id(), persona);
                    return persona;
                }

                @Override
                public Optional<Persona> findById(PersonaId id) {
                    return Optional.ofNullable(personaStore.get(id));
                }

                @Override
                public List<Persona> findAllByIds(Collection<PersonaId> ids) {
                    return ids.stream()
                            .map(personaStore::get)
                            .filter(java.util.Objects::nonNull)
                            .toList();
                }

                @Override
                public Optional<Persona> findByUsername(Username username) {
                    return personaStore.values().stream()
                            .filter(p -> p.username().equals(username))
                            .findFirst();
                }

                @Override
                public boolean existsByUsername(Username username) {
                    return findByUsername(username).isPresent();
                }

                @Override
                public boolean existsByDni(DniNumber dni) {
                    return personaStore.values().stream()
                            .anyMatch(p -> p.dni().number().equals(dni));
                }
            };

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

    private final PhotoStore photos =
            new PhotoStore() {
                @Override
                public PhotoKey put(RawPhoto photo) {
                    PhotoKey key = new PhotoKey("photo-" + (sequence++));
                    photoBytes.put(key, photo.content());
                    return key;
                }

                @Override
                public Optional<byte[]> get(PhotoKey key) {
                    return Optional.ofNullable(photoBytes.get(key));
                }
            };

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
    private final ListFamilyMembersQueryHandler listFamilyMembers =
            new ListFamilyMembersQueryHandler(families, memberships, personas);
    private final RemoveMemberCommandHandler removeMember =
            new RemoveMemberCommandHandler(families, memberships, clock);
    private final RevokeConsentCommandHandler revokeConsent =
            new RevokeConsentCommandHandler(families, memberships, clock);
    private final GetMemberDniQueryHandler getMemberDni =
            new GetMemberDniQueryHandler(families, memberships, personas);
    private final GetMemberDniPhotoQueryHandler getMemberDniPhoto =
            new GetMemberDniPhotoQueryHandler(families, memberships, personas, photos);

    private Persona savePersona(String username, String dni) {
        return personas.save(
                Persona.register(
                        PersonaId.generate(),
                        Username.of(username),
                        DniNumber.of(dni),
                        new PasswordHash("hashed:pw")));
    }

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

    @Test
    void listFamilyMembersVerifiesAcceptedViewerOrdersByDniExposesFlagsAndMasksRevokedDni() {
        Persona admin = savePersona("admin-user", "33333333C");
        Persona bob = savePersona("bob-user", "11111111A");
        Persona carol = savePersona("carol-user", "22222222B");
        Persona outsider = savePersona("outsider", "99999999Z");

        FamilyView family =
                createFamily.handle(new CreateFamilyCommand(admin.id().value(), "Casa"));
        IssueInviteResult inviteBob =
                issueInvite.handle(new IssueInviteCommand(admin.id().value(), family.id()));
        redeemInvite.handle(new RedeemInviteCommand(bob.id().value(), inviteBob.code()));
        IssueInviteResult inviteCarol =
                issueInvite.handle(new IssueInviteCommand(admin.id().value(), family.id()));
        redeemInvite.handle(new RedeemInviteCommand(carol.id().value(), inviteCarol.code()));

        Membership bobMembership =
                memberships
                        .findByFamilyAndPersona(new FamilyId(family.id()), bob.id())
                        .orElseThrow();
        bobMembership.revokeConsent(now.plusSeconds(5));
        memberships.save(bobMembership);

        assertThatThrownBy(() ->
                        listFamilyMembers.handle(
                                new ListFamilyMembersQuery(
                                        outsider.id().value(), family.id(), PageQuery.of(0, 20))))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() ->
                        listFamilyMembers.handle(
                                new ListFamilyMembersQuery(
                                        admin.id().value(),
                                        UUID.randomUUID(),
                                        PageQuery.of(0, 20))))
                .isInstanceOf(NotFoundException.class);

        PagedResult<MemberView> page =
                listFamilyMembers.handle(
                        new ListFamilyMembersQuery(
                                admin.id().value(), family.id(), PageQuery.of(0, 500)));

        assertThat(page.size()).isEqualTo(100);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.content())
                .containsExactly(
                        new MemberView(
                                bob.id().value(), "bob-user", null, false, "ACCEPTED", false),
                        new MemberView(
                                carol.id().value(),
                                "carol-user",
                                "22222222B",
                                false,
                                "ACCEPTED",
                                true),
                        new MemberView(
                                admin.id().value(),
                                "admin-user",
                                "33333333C",
                                true,
                                "ACCEPTED",
                                true));
    }

    @Test
    void removeMemberRequiresAdminVerifiesAcceptedTargetForbidsAdminRemovalAndRevokesConsent() {
        Persona admin = savePersona("admin-user", "11111111A");
        Persona member = savePersona("member-user", "22222222B");
        Persona outsider = savePersona("outsider", "33333333C");
        FamilyView family =
                createFamily.handle(new CreateFamilyCommand(admin.id().value(), "Casa"));
        IssueInviteResult invite =
                issueInvite.handle(new IssueInviteCommand(admin.id().value(), family.id()));
        redeemInvite.handle(new RedeemInviteCommand(member.id().value(), invite.code()));

        assertThatThrownBy(() ->
                        removeMember.handle(
                                new RemoveMemberCommand(
                                        member.id().value(), family.id(), admin.id().value())))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() ->
                        removeMember.handle(
                                new RemoveMemberCommand(
                                        admin.id().value(), family.id(), outsider.id().value())))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() ->
                        removeMember.handle(
                                new RemoveMemberCommand(
                                        admin.id().value(), family.id(), admin.id().value())))
                .isInstanceOf(CannotRemoveAdminException.class);

        now = now.plusSeconds(45);
        removeMember.handle(
                new RemoveMemberCommand(admin.id().value(), family.id(), member.id().value()));

        Membership removed =
                memberships
                        .findByFamilyAndPersona(new FamilyId(family.id()), member.id())
                        .orElseThrow();
        assertThat(removed.status()).isEqualTo(Membership.Status.REMOVED);
        assertThat(removed.isConsentInForce()).isFalse();
        assertThat(removed.consent().revokedAt()).contains(now);

        assertThatThrownBy(() ->
                        removeMember.handle(
                                new RemoveMemberCommand(
                                        admin.id().value(), family.id(), member.id().value())))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void revokeConsentAllowsSelfOnlyKeepsMembershipAcceptedAndLeavesOtherFamiliesUntouched() {
        Persona alice = savePersona("alice", "11111111A");
        Persona bob = savePersona("bob", "22222222B");
        Persona carol = savePersona("carol", "33333333C");

        FamilyView familyA =
                createFamily.handle(new CreateFamilyCommand(alice.id().value(), "Familia A"));
        IssueInviteResult inviteA =
                issueInvite.handle(new IssueInviteCommand(alice.id().value(), familyA.id()));
        redeemInvite.handle(new RedeemInviteCommand(bob.id().value(), inviteA.code()));

        FamilyView familyB =
                createFamily.handle(new CreateFamilyCommand(carol.id().value(), "Familia B"));
        IssueInviteResult inviteB =
                issueInvite.handle(new IssueInviteCommand(carol.id().value(), familyB.id()));
        redeemInvite.handle(new RedeemInviteCommand(bob.id().value(), inviteB.code()));

        assertThatThrownBy(() ->
                        revokeConsent.handle(
                                new RevokeConsentCommand(
                                        alice.id().value(), familyA.id(), bob.id().value())))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() ->
                        revokeConsent.handle(
                                new RevokeConsentCommand(
                                        bob.id().value(), UUID.randomUUID(), bob.id().value())))
                .isInstanceOf(NotFoundException.class);

        now = now.plusSeconds(30);
        revokeConsent.handle(
                new RevokeConsentCommand(bob.id().value(), familyA.id(), bob.id().value()));

        Membership inFamilyA =
                memberships
                        .findByFamilyAndPersona(new FamilyId(familyA.id()), bob.id())
                        .orElseThrow();
        Membership inFamilyB =
                memberships
                        .findByFamilyAndPersona(new FamilyId(familyB.id()), bob.id())
                        .orElseThrow();
        assertThat(inFamilyA.isAccepted()).isTrue();
        assertThat(inFamilyA.isConsentInForce()).isFalse();
        assertThat(inFamilyA.consent().revokedAt()).contains(now);
        assertThat(inFamilyB.isAccepted()).isTrue();
        assertThat(inFamilyB.isConsentInForce()).isTrue();
    }

    @Test
    void getMemberDniAndPhotoEnforceViewerTargetAndConsentVisibilityAndStreamStoredPhotoSides() {
        Persona admin = savePersona("admin-user", "11111111A");
        Persona member = savePersona("member-user", "22222222B");
        byte[] frontBytes = new byte[] {10, 20, 30};
        RawPhoto rawFront = RawPhoto.of("front", frontBytes, "image/png");
        DniPhoto storedFront = new DniPhoto(photos.put(rawFront), rawFront.contentType());
        member.changeDniPhotos(storedFront, null);
        personas.save(member);
        Persona outsider = savePersona("outsider", "33333333C");

        FamilyView family =
                createFamily.handle(new CreateFamilyCommand(admin.id().value(), "Casa"));
        IssueInviteResult invite =
                issueInvite.handle(new IssueInviteCommand(admin.id().value(), family.id()));
        redeemInvite.handle(new RedeemInviteCommand(member.id().value(), invite.code()));

        assertThatThrownBy(() ->
                        getMemberDni.handle(
                                new GetMemberDniQuery(
                                        outsider.id().value(), family.id(), member.id().value())))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() ->
                        getMemberDni.handle(
                                new GetMemberDniQuery(
                                        admin.id().value(),
                                        UUID.randomUUID(),
                                        member.id().value())))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() ->
                        getMemberDni.handle(
                                new GetMemberDniQuery(
                                        admin.id().value(), family.id(), UUID.randomUUID())))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() ->
                        getMemberDni.handle(
                                new GetMemberDniQuery(
                                        admin.id().value(), family.id(), outsider.id().value())))
                .isInstanceOf(NotFoundException.class);

        MemberDniView visible =
                getMemberDni.handle(
                        new GetMemberDniQuery(
                                admin.id().value(), family.id(), member.id().value()));
        assertThat(visible)
                .isEqualTo(
                        new MemberDniView(
                                family.id(), member.id().value(), "22222222B", true, false));

        DniPhotoView front =
                getMemberDniPhoto.handle(
                        new GetMemberDniPhotoQuery(
                                admin.id().value(), family.id(), member.id().value(), Side.FRONT));
        assertThat(front.content()).isEqualTo(frontBytes);
        assertThat(front.contentType()).isEqualTo("image/png");

        assertThatThrownBy(() ->
                        getMemberDniPhoto.handle(
                                new GetMemberDniPhotoQuery(
                                        admin.id().value(),
                                        family.id(),
                                        member.id().value(),
                                        Side.BACK)))
                .isInstanceOf(NotFoundException.class);

        revokeConsent.handle(
                new RevokeConsentCommand(member.id().value(), family.id(), member.id().value()));
        assertThatThrownBy(() ->
                        getMemberDni.handle(
                                new GetMemberDniQuery(
                                        admin.id().value(), family.id(), member.id().value())))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() ->
                        getMemberDniPhoto.handle(
                                new GetMemberDniPhotoQuery(
                                        admin.id().value(),
                                        family.id(),
                                        member.id().value(),
                                        Side.FRONT)))
                .isInstanceOf(ForbiddenException.class);

        IssueInviteResult reInvite =
                issueInvite.handle(new IssueInviteCommand(admin.id().value(), family.id()));
        redeemInvite.handle(new RedeemInviteCommand(member.id().value(), reInvite.code()));
        assertThat(
                        getMemberDni
                                .handle(
                                        new GetMemberDniQuery(
                                                admin.id().value(),
                                                family.id(),
                                                member.id().value()))
                                .number())
                .isEqualTo("22222222B");

        removeMember.handle(
                new RemoveMemberCommand(admin.id().value(), family.id(), member.id().value()));
        assertThatThrownBy(() ->
                        getMemberDni.handle(
                                new GetMemberDniQuery(
                                        admin.id().value(), family.id(), member.id().value())))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() ->
                        getMemberDni.handle(
                                new GetMemberDniQuery(
                                        member.id().value(), family.id(), admin.id().value())))
                .isInstanceOf(ForbiddenException.class);
    }
}
