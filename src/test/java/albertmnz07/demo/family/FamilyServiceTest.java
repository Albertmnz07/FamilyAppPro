package albertmnz07.demo.family;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({FamilyService.class, InviteCodeGenerator.class})
class FamilyServiceTest {

    @Autowired
    FamilyService familyService;

    @Autowired
    PersonaRepository personaRepository;

    @Autowired
    InviteRepository inviteRepository;

    @Test
    void creatingAFamilyPersistsItWithTheCreatorAsAdmin() {
        Persona creator = personaRepository.save(new Persona("12345678Z"));

        Family family = familyService.createFamily(creator, "Familia García");

        Family persisted = familyService.getFamily(family.getId());
        assertThat(persisted.getName()).isEqualTo("Familia García");
        assertThat(persisted.getAdmin().getId()).isEqualTo(creator.getId());
    }

    @Test
    void issuingAnInviteCreatesASixCharacterAlphanumericCodeExpiringInSevenDays() {
        Persona creator = personaRepository.save(new Persona("12345678Z"));
        Family family = familyService.createFamily(creator, null);
        Instant before = Instant.now();

        Invite invite = familyService.issueInvite(family);

        assertThat(invite.getCode()).hasSize(6);
        assertThat(invite.getCode()).matches("[A-Z0-9]{6}");
        assertThat(invite.getExpiresAt())
                .isBetween(before.plus(Duration.ofDays(7)), Instant.now().plus(Duration.ofDays(7)));
        assertThat(inviteRepository.findById(invite.getId())).isPresent();
    }
}
