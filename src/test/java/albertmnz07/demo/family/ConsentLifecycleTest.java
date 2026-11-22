package albertmnz07.demo.family;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({FamilyService.class, InviteCodeGenerator.class})
class ConsentLifecycleTest {

    @Autowired FamilyService familyService;

    @Autowired PersonaRepository personaRepository;

    @Test
    void revokingConsentImmediatelyDropsVisibility() {
        Persona admin =
                personaRepository.save(new Persona("admin", "11111111A", "front-key", "back-key"));
        Family family = familyService.createFamily(admin, "Familia García");
        Invite invite = familyService.issueInvite(family);
        Persona member =
                personaRepository.save(new Persona("member", "22222222B", "front-key", "back-key"));
        familyService.redeemInvite(invite.getCode(), member);
        assertThat(familyService.visibleDni(family, admin, member)).isPresent();

        familyService.revokeConsent(family, member);

        assertThat(familyService.visibleDni(family, admin, member)).isEmpty();
    }

    @Test
    void reAcceptanceReEstablishesConsent() {
        Persona admin =
                personaRepository.save(new Persona("admin", "11111111A", "front-key", "back-key"));
        Family family = familyService.createFamily(admin, "Familia García");
        Invite invite = familyService.issueInvite(family);
        Persona member =
                personaRepository.save(new Persona("member", "22222222B", "front-key", "back-key"));
        familyService.redeemInvite(invite.getCode(), member);
        familyService.revokeConsent(family, member);
        assertThat(familyService.visibleDni(family, admin, member)).isEmpty();

        Invite renewedInvite = familyService.issueInvite(family);
        familyService.redeemInvite(renewedInvite.getCode(), member);

        assertThat(familyService.visibleDni(family, admin, member)).isPresent();
    }

    @Test
    void consentAndVisibilityArePerFamily() {
        Persona alice =
                personaRepository.save(new Persona("admin", "11111111A", "front-key", "back-key"));
        Family familyA = familyService.createFamily(alice, "Familia A");
        Invite inviteA = familyService.issueInvite(familyA);
        Persona bob =
                personaRepository.save(new Persona("member", "22222222B", "front-key", "back-key"));
        familyService.redeemInvite(inviteA.getCode(), bob);
        Persona carol =
                personaRepository.save(new Persona("carol", "33333333C", "front-key", "back-key"));
        Family familyB = familyService.createFamily(carol, "Familia B");
        Invite inviteB = familyService.issueInvite(familyB);
        familyService.redeemInvite(inviteB.getCode(), bob);

        assertThat(familyService.visibleDni(familyA, alice, bob)).isPresent();
        assertThat(familyService.visibleDni(familyB, carol, bob)).isPresent();

        familyService.revokeConsent(familyA, bob);

        assertThat(familyService.visibleDni(familyA, alice, bob)).isEmpty();
        assertThat(familyService.visibleDni(familyB, carol, bob)).isPresent();
    }

    @Test
    void consentIsWithheldUntilTheInviteIsAccepted() {
        Persona alice =
                personaRepository.save(new Persona("admin", "11111111A", "front-key", "back-key"));
        Family family = familyService.createFamily(alice, "Familia A");
        Invite invite = familyService.issueInvite(family);
        Persona bob =
                personaRepository.save(new Persona("member", "22222222B", "front-key", "back-key"));

        assertThat(familyService.visibleDni(family, alice, bob)).isEmpty();

        familyService.redeemInvite(invite.getCode(), bob);

        assertThat(familyService.visibleDni(family, alice, bob)).isPresent();
    }
}
