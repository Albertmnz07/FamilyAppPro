package albertmnz07.familyapp.domain.model;

import albertmnz07.familyapp.domain.exception.InviteAlreadyRedeemedException;
import albertmnz07.familyapp.domain.exception.InviteExpiredException;
import albertmnz07.familyapp.domain.valueobject.FamilyId;
import albertmnz07.familyapp.domain.valueobject.InviteCode;
import albertmnz07.familyapp.domain.valueobject.InviteId;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Aggregate Root: a family-scoped, single-use, time-bound token redeemed to join a family. */
public final class Invite {

    private final InviteId id;
    private final InviteCode code;
    private final FamilyId familyId;
    private final Instant expiresAt;
    private Instant redeemedAt;

    private Invite(
            InviteId id,
            InviteCode code,
            FamilyId familyId,
            Instant expiresAt,
            Instant redeemedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.code = Objects.requireNonNull(code, "code");
        this.familyId = Objects.requireNonNull(familyId, "familyId");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.redeemedAt = redeemedAt;
    }

    public static Invite issue(
            InviteCode code, FamilyId familyId, Instant now, Duration timeToLive) {
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(timeToLive, "timeToLive");
        return new Invite(InviteId.generate(), code, familyId, now.plus(timeToLive), null);
    }

    public static Invite restore(
            InviteId id,
            InviteCode code,
            FamilyId familyId,
            Instant expiresAt,
            Instant redeemedAt) {
        return new Invite(id, code, familyId, expiresAt, redeemedAt);
    }

    public boolean isExpiredAt(Instant now) {
        Objects.requireNonNull(now, "now");
        return !now.isBefore(expiresAt);
    }

    public boolean isRedeemed() {
        return redeemedAt != null;
    }

    /** Marks the invite redeemed at {@code now}, refusing expired or already-redeemed tokens. */
    public void redeem(Instant now) {
        Objects.requireNonNull(now, "now");
        if (isExpiredAt(now)) {
            throw new InviteExpiredException("Invite has expired: " + code.value());
        }
        if (isRedeemed()) {
            throw new InviteAlreadyRedeemedException(
                    "Invite has already been redeemed: " + code.value());
        }
        this.redeemedAt = now;
    }

    public InviteId id() {
        return id;
    }

    public InviteCode code() {
        return code;
    }

    public FamilyId familyId() {
        return familyId;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Optional<Instant> redeemedAt() {
        return Optional.ofNullable(redeemedAt);
    }
}
