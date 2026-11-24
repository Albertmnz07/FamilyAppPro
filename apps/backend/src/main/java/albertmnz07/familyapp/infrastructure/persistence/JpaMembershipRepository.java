package albertmnz07.familyapp.infrastructure.persistence;

import albertmnz07.familyapp.domain.model.Consent;
import albertmnz07.familyapp.domain.model.Membership;
import albertmnz07.familyapp.domain.repository.MembershipRepository;
import albertmnz07.familyapp.domain.valueobject.ConsentId;
import albertmnz07.familyapp.domain.valueobject.FamilyId;
import albertmnz07.familyapp.domain.valueobject.MembershipId;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * Persists and reconstructs the {@link Membership} aggregate across both the {@code membership} and
 * {@code consent} tables so schema compatibility holds while {@link Consent} stays encapsulated.
 */
@Repository
public class JpaMembershipRepository implements MembershipRepository {

    private final MembershipJpaRepository memberships;
    private final ConsentJpaRepository consents;

    public JpaMembershipRepository(
            MembershipJpaRepository memberships, ConsentJpaRepository consents) {
        this.memberships = memberships;
        this.consents = consents;
    }

    @Override
    public Membership save(Membership membership) {
        MembershipJpaEntity savedMembership = memberships.save(toMembershipEntity(membership));
        ConsentJpaEntity savedConsent = consents.save(toConsentEntity(membership));
        return toDomain(savedMembership, savedConsent);
    }

    @Override
    public Optional<Membership> findByFamilyAndPersona(FamilyId familyId, PersonaId personaId) {
        return memberships
                .findByFamilyIdAndPersonaId(familyId.value(), personaId.value())
                .map(membership -> {
                    ConsentJpaEntity consent =
                            consents.findByFamilyIdAndPersonaId(familyId.value(), personaId.value())
                                    .orElseThrow(() ->
                                            new IllegalStateException(
                                                    "Missing consent row for membership "
                                                            + membership.getId()));
                    return toDomain(membership, consent);
                });
    }

    @Override
    public boolean existsAccepted(FamilyId familyId, PersonaId personaId) {
        return memberships.existsByFamilyIdAndPersonaIdAndStatus(
                familyId.value(), personaId.value(), Membership.Status.ACCEPTED);
    }

    @Override
    public List<Membership> findAcceptedByPersona(PersonaId personaId) {
        List<MembershipJpaEntity> rows =
                memberships.findByPersonaIdAndStatus(personaId.value(), Membership.Status.ACCEPTED);
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<UUID, ConsentJpaEntity> consentsByFamily =
                consents.findByPersonaId(personaId.value()).stream()
                        .collect(
                                Collectors.toMap(
                                        ConsentJpaEntity::getFamilyId, Function.identity()));
        return rows.stream()
                .map(row ->
                        toDomain(
                                row,
                                requireConsent(
                                        consentsByFamily.get(row.getFamilyId()), row.getId())))
                .toList();
    }

    @Override
    public List<Membership> findAcceptedByFamily(FamilyId familyId) {
        List<MembershipJpaEntity> rows =
                memberships.findByFamilyIdAndStatus(familyId.value(), Membership.Status.ACCEPTED);
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<UUID, ConsentJpaEntity> consentsByPersona =
                consents.findByFamilyId(familyId.value()).stream()
                        .collect(
                                Collectors.toMap(
                                        ConsentJpaEntity::getPersonaId, Function.identity()));
        return rows.stream()
                .map(row ->
                        toDomain(
                                row,
                                requireConsent(
                                        consentsByPersona.get(row.getPersonaId()), row.getId())))
                .toList();
    }

    private static ConsentJpaEntity requireConsent(ConsentJpaEntity consent, UUID membershipId) {
        if (consent == null) {
            throw new IllegalStateException("Missing consent row for membership " + membershipId);
        }
        return consent;
    }

    private static MembershipJpaEntity toMembershipEntity(Membership membership) {
        return new MembershipJpaEntity(
                membership.id().value(),
                membership.familyId().value(),
                membership.personaId().value(),
                membership.status());
    }

    private static ConsentJpaEntity toConsentEntity(Membership membership) {
        Consent consent = membership.consent();
        return new ConsentJpaEntity(
                consent.id().value(),
                membership.familyId().value(),
                membership.personaId().value(),
                consent.grantedAt(),
                consent.revokedAt().orElse(null));
    }

    private static Membership toDomain(MembershipJpaEntity membership, ConsentJpaEntity consent) {
        return Membership.restore(
                new MembershipId(membership.getId()),
                new FamilyId(membership.getFamilyId()),
                new PersonaId(membership.getPersonaId()),
                membership.getStatus(),
                Consent.restore(
                        new ConsentId(consent.getId()),
                        consent.getGrantedAt(),
                        consent.getRevokedAt()));
    }
}
