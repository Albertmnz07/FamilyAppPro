package albertmnz07.familyapp.infrastructure.persistence;

import albertmnz07.familyapp.domain.model.Family;
import albertmnz07.familyapp.domain.repository.FamilyRepository;
import albertmnz07.familyapp.domain.valueobject.FamilyId;
import albertmnz07.familyapp.domain.valueobject.FamilyName;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaFamilyRepository implements FamilyRepository {

    private final FamilyJpaRepository jpa;

    public JpaFamilyRepository(FamilyJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Family save(Family family) {
        return toDomain(jpa.save(toEntity(family)));
    }

    @Override
    public Optional<Family> findById(FamilyId id) {
        return jpa.findById(id.value()).map(JpaFamilyRepository::toDomain);
    }

    @Override
    public List<Family> findAllByIds(Collection<FamilyId> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jpa.findAllById(ids.stream().map(FamilyId::value).toList()).stream()
                .map(JpaFamilyRepository::toDomain)
                .toList();
    }

    private static FamilyJpaEntity toEntity(Family family) {
        return new FamilyJpaEntity(
                family.id().value(), family.name().value(), family.adminId().value());
    }

    private static Family toDomain(FamilyJpaEntity entity) {
        return Family.restore(
                new FamilyId(entity.getId()),
                new FamilyName(entity.getName()),
                new PersonaId(entity.getAdminId()));
    }
}
