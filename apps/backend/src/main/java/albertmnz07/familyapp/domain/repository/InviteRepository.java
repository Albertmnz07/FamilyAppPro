package albertmnz07.familyapp.domain.repository;

import albertmnz07.familyapp.domain.model.Invite;
import albertmnz07.familyapp.domain.valueobject.InviteCode;
import java.util.Optional;

public interface InviteRepository {

    Invite save(Invite invite);

    boolean existsByCode(InviteCode code);

    /** Acquires an exclusive write lock on the invite row so concurrent redemptions serialize. */
    Optional<Invite> findByCodeForUpdate(InviteCode code);
}
