package albertmnz07.familyapp.infrastructure.persistence;

import albertmnz07.familyapp.domain.model.Invite;
import albertmnz07.familyapp.domain.repository.InviteRepository;
import albertmnz07.familyapp.domain.valueobject.FamilyId;
import albertmnz07.familyapp.domain.valueobject.InviteCode;
import albertmnz07.familyapp.domain.valueobject.InviteId;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaInviteRepository implements InviteRepository {

    private final InviteJpaRepository jpa;

    public JpaInviteRepository(InviteJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Invite save(Invite invite) {
        return toDomain(jpa.save(toEntity(invite)));
    }

    @Override
    public boolean existsByCode(InviteCode code) {
        return jpa.existsByCode(code.value());
    }

    @Override
    public Optional<Invite> findByCodeForUpdate(InviteCode code) {
        return jpa.findByCodeForUpdate(code.value()).map(JpaInviteRepository::toDomain);
    }

    private static InviteJpaEntity toEntity(Invite invite) {
        return new InviteJpaEntity(
                invite.id().value(),
                invite.code().value(),
                invite.familyId().value(),
                invite.expiresAt(),
                invite.redeemedAt().orElse(null));
    }

    private static Invite toDomain(InviteJpaEntity entity) {
        return Invite.restore(
                new InviteId(entity.getId()),
                new InviteCode(entity.getCode()),
                new FamilyId(entity.getFamilyId()),
                entity.getExpiresAt(),
                entity.getRedeemedAt());
    }
}
