package albertmnz07.demo.family;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({FamilyService.class, InviteCodeGenerator.class})
class MemberRemovalTest {

    private static final byte[] FRONT = {1, 2, 3};
    private static final byte[] BACK = {4, 5, 6};

    @Autowired
    FamilyService familyService;

    @Autowired
    PersonaRepository personaRepository;

    @Test
    void removingAMemberDropsTheirVisibility() {
        Persona admin = savePersona("11111111A");
        Family family = familyService.createFamily(admin, "Familia García");
        Persona member = savePersona("22222222B");
        redeemInto(family, member);
        assertThat(familyService.visibleDni(family, admin, member)).isPresent();

        familyService.removeMember(family, member);

        assertThat(familyService.visibleDni(family, admin, member)).isEmpty();
    }

    @Test
    void aRemovedMemberIsNoLongerACurrentMember() {
        Persona admin = savePersona("11111111A");
        Family family = familyService.createFamily(admin, "Familia García");
        Persona member = savePersona("22222222B");
        redeemInto(family, member);
        assertThat(familyService.visibleDni(family, member, admin)).isPresent();

        familyService.removeMember(family, member);

        assertThat(familyService.visibleDni(family, member, admin)).isEmpty();
    }

    @Test
    void aRemovedMemberCanRejoinThroughAFreshInvite() {
        Persona admin = savePersona("11111111A");
        Family family = familyService.createFamily(admin, "Familia García");
        Persona member = savePersona("22222222B");
        Invite invite = familyService.issueInvite(family);
        familyService.redeemInvite(invite.getCode(), member);
        familyService.removeMember(family, member);

        assertThatThrownBy(() -> familyService.redeemInvite(invite.getCode(), member))
                .isInstanceOf(IllegalStateException.class);
        assertThat(familyService.visibleDni(family, admin, member)).isEmpty();

        redeemInto(family, member);

        assertThat(familyService.visibleDni(family, admin, member)).isPresent();
        assertThat(familyService.visibleDni(family, member, admin)).isPresent();
    }

    @Test
    void theAdminCannotBeRemoved() {
        Persona admin = savePersona("11111111A");
        Family family = familyService.createFamily(admin, "Familia García");

        assertThatThrownBy(() -> familyService.removeMember(family, admin))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(familyService.visibleDni(family, admin, admin)).isPresent();
    }

    private Persona savePersona(String dni) {
        return personaRepository.save(new Persona(dni, FRONT, BACK));
    }

    private void redeemInto(Family family, Persona persona) {
        Invite invite = familyService.issueInvite(family);
        familyService.redeemInvite(invite.getCode(), persona);
    }
}
