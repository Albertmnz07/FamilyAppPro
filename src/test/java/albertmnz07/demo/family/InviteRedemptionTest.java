package albertmnz07.demo.family;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({FamilyService.class, InviteCodeGenerator.class})
class InviteRedemptionTest {

    private static final byte[] FRONT = {1, 2, 3};
    private static final byte[] BACK = {4, 5, 6};

    @Autowired
    FamilyService familyService;

    @Autowired
    PersonaRepository personaRepository;

    @Autowired
    InviteRepository inviteRepository;

    @Test
    void redeemingAValidInviteAcceptsTheMembership() {
        Persona admin = personaRepository.save(new Persona("11111111A", FRONT, BACK));
        Family family = familyService.createFamily(admin, "Familia García");
        Invite invite = familyService.issueInvite(family);
        Persona invitee = personaRepository.save(new Persona("22222222B", FRONT, BACK));

        Membership membership = familyService.redeemInvite(invite.getCode(), invitee);

        assertThat(membership.getStatus()).isEqualTo(Membership.Status.ACCEPTED);
        assertThat(membership.getFamily().getId()).isEqualTo(family.getId());
        assertThat(membership.getPersona().getId()).isEqualTo(invitee.getId());
    }

    @Test
    void acceptingAnInviteGrantsConsentSoFamilyMembersCanSeeTheDniNumberAndPhotos() {
        Persona admin = personaRepository.save(new Persona("11111111A", FRONT, BACK));
        Family family = familyService.createFamily(admin, "Familia García");
        Invite invite = familyService.issueInvite(family);
        Persona invitee = personaRepository.save(new Persona("22222222B", FRONT, BACK));

        familyService.redeemInvite(invite.getCode(), invitee);

        Optional<Dni> visible = familyService.visibleDni(family, admin, invitee);
        assertThat(visible).isPresent();
        assertThat(visible.get().number()).isEqualTo("22222222B");
        assertThat(visible.get().frontPhoto()).isEqualTo(FRONT);
        assertThat(visible.get().backPhoto()).isEqualTo(BACK);
    }

    @Test
    void redeemingAnExpiredInviteIsRejected() {
        Persona admin = personaRepository.save(new Persona("11111111A", FRONT, BACK));
        Family family = familyService.createFamily(admin, "Familia García");
        Persona invitee = personaRepository.save(new Persona("22222222B", FRONT, BACK));
        Invite expired = inviteRepository.save(new Invite("EXPIRY", family, Instant.now().minusSeconds(60)));

        assertThatThrownBy(() -> familyService.redeemInvite(expired.getCode(), invitee))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void anInviteCanOnlyBeRedeemedOnce() {
        Persona admin = personaRepository.save(new Persona("11111111A", FRONT, BACK));
        Family family = familyService.createFamily(admin, "Familia García");
        Invite invite = familyService.issueInvite(family);
        Persona first = personaRepository.save(new Persona("22222222B", FRONT, BACK));
        Persona second = personaRepository.save(new Persona("33333333C", FRONT, BACK));
        familyService.redeemInvite(invite.getCode(), first);

        assertThatThrownBy(() -> familyService.redeemInvite(invite.getCode(), second))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void theCreatorIsVisibleToOtherFamilyMembers() {
        Persona admin = personaRepository.save(new Persona("11111111A", FRONT, BACK));
        Family family = familyService.createFamily(admin, "Familia García");
        Invite invite = familyService.issueInvite(family);
        Persona invitee = personaRepository.save(new Persona("22222222B", FRONT, BACK));
        familyService.redeemInvite(invite.getCode(), invitee);

        Optional<Dni> visible = familyService.visibleDni(family, invitee, admin);
        assertThat(visible).isPresent();
        assertThat(visible.get().number()).isEqualTo("11111111A");
        assertThat(visible.get().frontPhoto()).isEqualTo(FRONT);
        assertThat(visible.get().backPhoto()).isEqualTo(BACK);
    }

    @Test
    void aPersonaOutsideTheFamilyCannotSeeItsMembers() {
        Persona admin = personaRepository.save(new Persona("11111111A", FRONT, BACK));
        Family family = familyService.createFamily(admin, "Familia García");
        Invite invite = familyService.issueInvite(family);
        Persona invitee = personaRepository.save(new Persona("22222222B", FRONT, BACK));
        familyService.redeemInvite(invite.getCode(), invitee);
        Persona outsider = personaRepository.save(new Persona("44444444D", FRONT, BACK));

        assertThat(familyService.visibleDni(family, outsider, invitee)).isEmpty();
    }

    @Test
    void redeemingAnUnknownInviteIsRejected() {
        Persona invitee = personaRepository.save(new Persona("22222222B", FRONT, BACK));

        assertThatThrownBy(() -> familyService.redeemInvite("ZZZZZZ", invitee))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void consentIsScopedToTheFamilyWhereItWasGranted() {
        Persona alice = personaRepository.save(new Persona("11111111A", FRONT, BACK));
        Family familyA = familyService.createFamily(alice, "Familia A");
        Invite inviteA = familyService.issueInvite(familyA);
        Persona bob = personaRepository.save(new Persona("22222222B", FRONT, BACK));
        familyService.redeemInvite(inviteA.getCode(), bob);
        Persona carol = personaRepository.save(new Persona("33333333C", FRONT, BACK));
        Family familyB = familyService.createFamily(carol, "Familia B");

        assertThat(familyService.visibleDni(familyA, alice, bob)).isPresent();
        assertThat(familyService.visibleDni(familyB, carol, bob)).isEmpty();
    }
}
