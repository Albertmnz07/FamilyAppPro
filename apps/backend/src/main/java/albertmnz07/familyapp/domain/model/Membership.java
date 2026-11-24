package albertmnz07.familyapp.domain.model;

import albertmnz07.familyapp.domain.exception.InvalidValueException;
import albertmnz07.familyapp.domain.valueobject.Dni;
import albertmnz07.familyapp.domain.valueobject.FamilyId;
import albertmnz07.familyapp.domain.valueobject.MembershipId;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregate Root: the link between a persona and a family, encapsulating {@link Consent} so
 * accepting a membership grants consent and removing a member revokes it (ADR-0001, ADR-0010).
 */
public final class Membership {

    public enum Status {
        ACCEPTED,
        REMOVED
    }

    private final MembershipId id;
    private final FamilyId familyId;
    private final PersonaId personaId;
    private Status status;
    private final Consent consent;

    private Membership(
            MembershipId id,
            FamilyId familyId,
            PersonaId personaId,
            Status status,
            Consent consent) {
        this.id = Objects.requireNonNull(id, "id");
        this.familyId = Objects.requireNonNull(familyId, "familyId");
        this.personaId = Objects.requireNonNull(personaId, "personaId");
        this.status = Objects.requireNonNull(status, "status");
        this.consent = Objects.requireNonNull(consent, "consent");
        if (status == Status.REMOVED && consent.isInForce()) {
            throw new InvalidValueException("A removed membership cannot hold consent in force");
        }
    }

    public static Membership acceptNew(FamilyId familyId, PersonaId personaId, Instant now) {
        return new Membership(
                MembershipId.generate(),
                familyId,
                personaId,
                Status.ACCEPTED,
                Consent.grantNew(now));
    }

    public static Membership restore(
            MembershipId id,
            FamilyId familyId,
            PersonaId personaId,
            Status status,
            Consent consent) {
        return new Membership(id, familyId, personaId, status, consent);
    }

    /** Accepts or re-accepts the membership and grants consent in the same step. */
    public void accept(Instant now) {
        this.status = Status.ACCEPTED;
        this.consent.grant(now);
    }

    /** Revokes consent immediately while keeping the membership accepted. */
    public void revokeConsent(Instant now) {
        this.consent.revoke(now);
    }

    /** Marks the membership removed and revokes consent if still in force. */
    public void remove(Instant now) {
        this.status = Status.REMOVED;
        this.consent.revoke(now);
    }

    public boolean isAccepted() {
        return status == Status.ACCEPTED;
    }

    public boolean isConsentInForce() {
        return isAccepted() && consent.isInForce();
    }

    /** Returns {@code dni} only while this membership is accepted and consent is in force. */
    public Optional<Dni> visibleDni(Dni dni) {
        Objects.requireNonNull(dni, "dni");
        return isConsentInForce() ? Optional.of(dni) : Optional.empty();
    }

    public MembershipId id() {
        return id;
    }

    public FamilyId familyId() {
        return familyId;
    }

    public PersonaId personaId() {
        return personaId;
    }

    public Status status() {
        return status;
    }

    public Consent consent() {
        return consent;
    }
}
