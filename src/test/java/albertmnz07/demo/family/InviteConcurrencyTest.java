package albertmnz07.demo.family;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({FamilyService.class, InviteCodeGenerator.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InviteConcurrencyTest {

    private static final byte[] FRONT = {1, 2, 3};
    private static final byte[] BACK = {4, 5, 6};

    @Autowired
    FamilyService familyService;

    @Autowired
    PersonaRepository personaRepository;

    @Autowired
    FamilyRepository familyRepository;

    @Autowired
    InviteRepository inviteRepository;

    @Autowired
    MembershipRepository membershipRepository;

    @Autowired
    ConsentRepository consentRepository;

    @AfterEach
    void cleanUp() {
        consentRepository.deleteAll();
        membershipRepository.deleteAll();
        inviteRepository.deleteAll();
        familyRepository.deleteAll();
        personaRepository.deleteAll();
    }

    @Test
    void concurrentRedemptionsOfTheSameInviteAcceptExactlyOnePersona() throws Exception {
        Persona admin = personaRepository.save(new Persona("admin", "11111111A", FRONT, BACK));
        Family family = familyService.createFamily(admin, "Familia García");
        Invite invite = familyService.issueInvite(family);
        Persona first = personaRepository.save(new Persona("member", "22222222B", FRONT, BACK));
        Persona second = personaRepository.save(new Persona("carol", "33333333C", FRONT, BACK));

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> firstAttempt = executor.submit(attempt(invite.getCode(), first, start));
            Future<Boolean> secondAttempt = executor.submit(attempt(invite.getCode(), second, start));
            start.countDown();

            List<Boolean> results = List.of(
                    firstAttempt.get(5, TimeUnit.SECONDS),
                    secondAttempt.get(5, TimeUnit.SECONDS));
            assertThat(results).containsExactlyInAnyOrder(true, false);
        } finally {
            executor.shutdownNow();
        }

        long visible = Stream.of(first, second)
                .filter(persona -> familyService.visibleDni(family, admin, persona).isPresent())
                .count();
        assertThat(visible).isEqualTo(1);
    }

    private Callable<Boolean> attempt(String code, Persona persona, CountDownLatch start) {
        return () -> {
            start.await();
            try {
                familyService.redeemInvite(code, persona);
                return true;
            } catch (IllegalStateException alreadyRedeemed) {
                return false;
            }
        };
    }
}
