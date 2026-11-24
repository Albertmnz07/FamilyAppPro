package albertmnz07.familyapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import albertmnz07.familyapp.domain.exception.CannotRemoveAdminException;
import albertmnz07.familyapp.domain.exception.ForbiddenException;
import albertmnz07.familyapp.domain.exception.InvalidValueException;
import albertmnz07.familyapp.domain.exception.InviteAlreadyRedeemedException;
import albertmnz07.familyapp.domain.exception.InviteExpiredException;
import albertmnz07.familyapp.domain.valueobject.ConsentId;
import albertmnz07.familyapp.domain.valueobject.Dni;
import albertmnz07.familyapp.domain.valueobject.DniNumber;
import albertmnz07.familyapp.domain.valueobject.DniPhoto;
import albertmnz07.familyapp.domain.valueobject.FamilyId;
import albertmnz07.familyapp.domain.valueobject.FamilyName;
import albertmnz07.familyapp.domain.valueobject.InviteCode;
import albertmnz07.familyapp.domain.valueobject.MembershipId;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import albertmnz07.familyapp.domain.valueobject.PhotoContentType;
import albertmnz07.familyapp.domain.valueobject.PhotoKey;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FamilyInviteAndMembershipTest {

    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    @Test
    void aCreatedFamilyRecognizesOnlyItsCreatorAsAdmin() {
        PersonaId adminId = PersonaId.generate();
        PersonaId otherId = PersonaId.generate();

        Family family = Family.create(FamilyName.of("Familia García"), adminId);

        assertThat(family.name()).isEqualTo(FamilyName.of("Familia García"));
        assertThat(family.adminId()).isEqualTo(adminId);
        assertThat(family.isAdmin(adminId)).isTrue();
        assertThat(family.isAdmin(otherId)).isFalse();
        assertThatCode(() -> family.requireAdmin(adminId)).doesNotThrowAnyException();
        assertThatThrownBy(() -> family.requireAdmin(otherId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void anIssuedInviteCanBeRedeemedOnceBeforeItsExpiry() {
        FamilyId familyId = FamilyId.generate();
        Invite invite = Invite.issue(InviteCode.of("ABC234"), familyId, NOW, Duration.ofDays(7));

        assertThat(invite.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        assertThat(invite.isRedeemed()).isFalse();
        assertThat(invite.isExpiredAt(NOW.plus(Duration.ofDays(7)).minusSeconds(1))).isFalse();
        assertThat(invite.isExpiredAt(NOW.plus(Duration.ofDays(7)))).isTrue();

        Instant redemptionTime = NOW.plusSeconds(60);
        invite.redeem(redemptionTime);

        assertThat(invite.isRedeemed()).isTrue();
        assertThat(invite.redeemedAt()).contains(redemptionTime);
        assertThatThrownBy(() -> invite.redeem(redemptionTime.plusSeconds(1)))
                .isInstanceOf(InviteAlreadyRedeemedException.class);
    }

    @Test
    void redeemingAnExpiredInviteIsRefused() {
        Invite invite =
                Invite.issue(InviteCode.of("ABC234"), FamilyId.generate(), NOW, Duration.ofDays(7));

        assertThatThrownBy(() -> invite.redeem(NOW.plus(Duration.ofDays(7))))
                .isInstanceOf(InviteExpiredException.class);
    }

    @Test
    void acceptingRevokingRemovingAndReAcceptingMembershipKeepsConsentInSync() {
        FamilyId familyId = FamilyId.generate();
        PersonaId personaId = PersonaId.generate();

        Membership membership = Membership.acceptNew(familyId, personaId, NOW);
        ConsentId originalConsentId = membership.consent().id();

        assertThat(membership.status()).isEqualTo(Membership.Status.ACCEPTED);
        assertThat(membership.isAccepted()).isTrue();
        assertThat(membership.isConsentInForce()).isTrue();
        assertThat(membership.consent().grantedAt()).isEqualTo(NOW);
        assertThat(membership.consent().revokedAt()).isEmpty();

        Instant revokedAt = NOW.plusSeconds(30);
        membership.revokeConsent(revokedAt);
        assertThat(membership.isAccepted()).isTrue();
        assertThat(membership.isConsentInForce()).isFalse();
        assertThat(membership.consent().revokedAt()).contains(revokedAt);

        Instant reAcceptedAt = NOW.plusSeconds(60);
        membership.accept(reAcceptedAt);
        assertThat(membership.isAccepted()).isTrue();
        assertThat(membership.isConsentInForce()).isTrue();
        assertThat(membership.consent().id()).isEqualTo(originalConsentId);
        assertThat(membership.consent().grantedAt()).isEqualTo(reAcceptedAt);
        assertThat(membership.consent().revokedAt()).isEmpty();

        Instant removedAt = NOW.plusSeconds(90);
        membership.remove(removedAt);
        assertThat(membership.status()).isEqualTo(Membership.Status.REMOVED);
        assertThat(membership.isAccepted()).isFalse();
        assertThat(membership.isConsentInForce()).isFalse();
        assertThat(membership.consent().isInForce()).isFalse();
        assertThat(membership.consent().revokedAt()).contains(removedAt);

        Instant rejoinedAt = NOW.plusSeconds(120);
        membership.accept(rejoinedAt);
        assertThat(membership.status()).isEqualTo(Membership.Status.ACCEPTED);
        assertThat(membership.isConsentInForce()).isTrue();
        assertThat(membership.consent().id()).isEqualTo(originalConsentId);
    }

    @Test
    void aRemovedMembershipCannotBeRestoredWithConsentStillInForce() {
        Consent activeConsent = Consent.restore(ConsentId.generate(), NOW, null);

        assertThatThrownBy(() ->
                        Membership.restore(
                                MembershipId.generate(),
                                FamilyId.generate(),
                                PersonaId.generate(),
                                Membership.Status.REMOVED,
                                activeConsent))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void theFamilyAdminCannotBeRemovedWhileOrdinaryMembersCan() {
        PersonaId adminId = PersonaId.generate();
        PersonaId memberId = PersonaId.generate();
        Family family = Family.create(FamilyName.of("Familia García"), adminId);

        assertThatThrownBy(() -> family.requireRemovable(adminId))
                .isInstanceOf(CannotRemoveAdminException.class);
        assertThatCode(() -> family.requireRemovable(memberId)).doesNotThrowAnyException();
    }

    @Test
    void dniVisibilityFollowsPerFamilyAcceptedMembershipAndConsentInForce() {
        PersonaId personaId = PersonaId.generate();
        Dni dni =
                new Dni(
                        DniNumber.of("12345678z"),
                        new DniPhoto(new PhotoKey("front-1"), PhotoContentType.of("image/jpeg")),
                        null);

        Membership familyAMembership = Membership.acceptNew(FamilyId.generate(), personaId, NOW);
        Membership familyBMembership = Membership.acceptNew(FamilyId.generate(), personaId, NOW);

        assertThat(familyAMembership.visibleDni(dni)).contains(dni);
        assertThat(familyBMembership.visibleDni(dni)).contains(dni);

        familyAMembership.revokeConsent(NOW.plusSeconds(10));
        assertThat(familyAMembership.visibleDni(dni)).isEmpty();
        assertThat(familyBMembership.visibleDni(dni)).contains(dni);

        familyAMembership.accept(NOW.plusSeconds(20));
        assertThat(familyAMembership.visibleDni(dni)).contains(dni);

        familyAMembership.remove(NOW.plusSeconds(30));
        assertThat(familyAMembership.visibleDni(dni)).isEmpty();
        assertThat(familyBMembership.visibleDni(dni)).contains(dni);
    }
}
