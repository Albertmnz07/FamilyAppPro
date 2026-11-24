package albertmnz07.familyapp.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import albertmnz07.familyapp.application.command.CommandHandler;
import albertmnz07.familyapp.application.command.CreateFamilyCommand;
import albertmnz07.familyapp.application.command.IssueInviteCommand;
import albertmnz07.familyapp.application.command.IssueInviteResult;
import albertmnz07.familyapp.application.command.RedeemInviteCommand;
import albertmnz07.familyapp.application.command.RedeemInviteResult;
import albertmnz07.familyapp.application.command.RegisterPersonaCommand;
import albertmnz07.familyapp.application.port.InviteCodeGenerator;
import albertmnz07.familyapp.application.query.FamilyView;
import albertmnz07.familyapp.application.query.PersonaView;
import albertmnz07.familyapp.domain.exception.InviteAlreadyRedeemedException;
import albertmnz07.familyapp.domain.model.Membership;
import albertmnz07.familyapp.domain.repository.MembershipRepository;
import albertmnz07.familyapp.domain.valueobject.FamilyId;
import albertmnz07.familyapp.domain.valueobject.InviteCode;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InviteConcurrencyTest {

    @Autowired CommandHandler<RegisterPersonaCommand, PersonaView> registerPersona;

    @Autowired CommandHandler<CreateFamilyCommand, FamilyView> createFamily;

    @Autowired CommandHandler<IssueInviteCommand, IssueInviteResult> issueInvite;

    @Autowired CommandHandler<RedeemInviteCommand, RedeemInviteResult> redeemInvite;

    @Autowired MembershipRepository memberships;

    @Autowired InviteCodeGenerator codeGenerator;

    @Autowired AuthTokenJpaRepository tokens;

    @Autowired ConsentJpaRepository consents;

    @Autowired MembershipJpaRepository membershipRows;

    @Autowired InviteJpaRepository invites;

    @Autowired FamilyJpaRepository families;

    @Autowired PersonaJpaRepository personas;

    @AfterEach
    void cleanUp() {
        tokens.deleteAll();
        consents.deleteAll();
        membershipRows.deleteAll();
        invites.deleteAll();
        families.deleteAll();
        personas.deleteAll();
    }

    @Test
    void concurrentRedemptionsOfTheSameInviteAcceptExactlyOnePersona() throws Exception {
        PersonaView admin =
                registerPersona.handle(
                        new RegisterPersonaCommand("admin", "11111111A", "password123"));
        FamilyView family =
                createFamily.handle(new CreateFamilyCommand(admin.id(), "Familia García"));
        IssueInviteResult invite =
                issueInvite.handle(new IssueInviteCommand(admin.id(), family.id()));
        PersonaView first =
                registerPersona.handle(
                        new RegisterPersonaCommand("member", "22222222B", "password123"));
        PersonaView second =
                registerPersona.handle(
                        new RegisterPersonaCommand("carol", "33333333C", "password123"));

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> firstAttempt =
                    executor.submit(attempt(invite.code(), first.id(), start));
            Future<Boolean> secondAttempt =
                    executor.submit(attempt(invite.code(), second.id(), start));
            start.countDown();

            List<Boolean> results =
                    List.of(
                            firstAttempt.get(5, TimeUnit.SECONDS),
                            secondAttempt.get(5, TimeUnit.SECONDS));
            assertThat(results).containsExactlyInAnyOrder(true, false);
        } finally {
            executor.shutdownNow();
        }

        FamilyId familyId = new FamilyId(family.id());
        long acceptedWithConsent =
                Stream.of(first.id(), second.id())
                        .map(PersonaId::new)
                        .flatMap(id -> memberships.findByFamilyAndPersona(familyId, id).stream())
                        .filter(Membership::isConsentInForce)
                        .count();
        assertThat(acceptedWithConsent).isEqualTo(1);
    }

    @Test
    void membershipRepositoryPersistsAndReconstructsConsentAcrossLifecycleTransitions() {
        PersonaView admin =
                registerPersona.handle(
                        new RegisterPersonaCommand("admin2", "44444444D", "password123"));
        PersonaView member =
                registerPersona.handle(
                        new RegisterPersonaCommand("member2", "55555555E", "password123"));
        FamilyView family =
                createFamily.handle(new CreateFamilyCommand(admin.id(), "Familia Pérez"));
        IssueInviteResult invite =
                issueInvite.handle(new IssueInviteCommand(admin.id(), family.id()));
        redeemInvite.handle(new RedeemInviteCommand(member.id(), invite.code()));

        FamilyId familyId = new FamilyId(family.id());
        PersonaId memberId = new PersonaId(member.id());
        Instant base = Instant.parse("2030-06-01T00:00:00Z");

        Membership loaded = memberships.findByFamilyAndPersona(familyId, memberId).orElseThrow();
        assertThat(loaded.isAccepted()).isTrue();
        assertThat(loaded.isConsentInForce()).isTrue();

        loaded.revokeConsent(base);
        memberships.save(loaded);
        Membership afterRevoke =
                memberships.findByFamilyAndPersona(familyId, memberId).orElseThrow();
        assertThat(afterRevoke.isAccepted()).isTrue();
        assertThat(afterRevoke.isConsentInForce()).isFalse();

        afterRevoke.remove(base.plusSeconds(10));
        memberships.save(afterRevoke);
        Membership afterRemove =
                memberships.findByFamilyAndPersona(familyId, memberId).orElseThrow();
        assertThat(afterRemove.status()).isEqualTo(Membership.Status.REMOVED);
        assertThat(afterRemove.isConsentInForce()).isFalse();

        afterRemove.accept(base.plusSeconds(20));
        memberships.save(afterRemove);
        Membership afterRejoin =
                memberships.findByFamilyAndPersona(familyId, memberId).orElseThrow();
        assertThat(afterRejoin.isAccepted()).isTrue();
        assertThat(afterRejoin.isConsentInForce()).isTrue();
    }

    @Test
    void inviteCodeGeneratorEmitsSixCharacterUnambiguousCodes() {
        for (int i = 0; i < 100; i++) {
            InviteCode code = codeGenerator.generate();
            assertThat(code.value()).matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{6}");
        }
    }

    private Callable<Boolean> attempt(String code, UUID personaId, CountDownLatch start) {
        return () -> {
            start.await();
            try {
                redeemInvite.handle(new RedeemInviteCommand(personaId, code));
                return true;
            } catch (InviteAlreadyRedeemedException alreadyRedeemed) {
                return false;
            }
        };
    }
}
