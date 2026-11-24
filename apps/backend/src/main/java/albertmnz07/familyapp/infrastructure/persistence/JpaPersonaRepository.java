package albertmnz07.familyapp.infrastructure.persistence;

import albertmnz07.familyapp.domain.model.Persona;
import albertmnz07.familyapp.domain.repository.PersonaRepository;
import albertmnz07.familyapp.domain.valueobject.Dni;
import albertmnz07.familyapp.domain.valueobject.DniNumber;
import albertmnz07.familyapp.domain.valueobject.DniPhoto;
import albertmnz07.familyapp.domain.valueobject.PasswordHash;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import albertmnz07.familyapp.domain.valueobject.PhotoContentType;
import albertmnz07.familyapp.domain.valueobject.PhotoKey;
import albertmnz07.familyapp.domain.valueobject.Username;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPersonaRepository implements PersonaRepository {

    private final PersonaJpaRepository jpa;

    public JpaPersonaRepository(PersonaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Persona save(Persona persona) {
        return toDomain(jpa.save(toEntity(persona)));
    }

    @Override
    public Optional<Persona> findById(PersonaId id) {
        return jpa.findById(id.value()).map(JpaPersonaRepository::toDomain);
    }

    @Override
    public List<Persona> findAllByIds(Collection<PersonaId> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jpa.findAllById(ids.stream().map(PersonaId::value).toList()).stream()
                .map(JpaPersonaRepository::toDomain)
                .toList();
    }

    @Override
    public Optional<Persona> findByUsername(Username username) {
        return jpa.findByUsername(username.value()).map(JpaPersonaRepository::toDomain);
    }

    @Override
    public boolean existsByUsername(Username username) {
        return jpa.existsByUsername(username.value());
    }

    @Override
    public boolean existsByDni(DniNumber dni) {
        return jpa.existsByDni(dni.value());
    }

    private static PersonaJpaEntity toEntity(Persona persona) {
        Dni dni = persona.dni();
        return new PersonaJpaEntity(
                persona.id().value(),
                persona.username().value(),
                dni.number().value(),
                persona.passwordHash().map(PasswordHash::value).orElse(null),
                dni.front().map(photo -> photo.key().value()).orElse(null),
                dni.front().map(photo -> photo.contentType().value()).orElse(null),
                dni.back().map(photo -> photo.key().value()).orElse(null),
                dni.back().map(photo -> photo.contentType().value()).orElse(null));
    }

    private static Persona toDomain(PersonaJpaEntity entity) {
        Dni dni =
                new Dni(
                        new DniNumber(entity.getDni()),
                        photoOf(entity.getFrontPhotoKey(), entity.getFrontPhotoContentType()),
                        photoOf(entity.getBackPhotoKey(), entity.getBackPhotoContentType()));
        String hash = entity.getPasswordHash();
        return Persona.restore(
                new PersonaId(entity.getId()),
                new Username(entity.getUsername()),
                dni,
                hash == null || hash.isBlank() ? null : new PasswordHash(hash));
    }

    private static DniPhoto photoOf(String key, String contentType) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return new DniPhoto(new PhotoKey(key), PhotoContentType.restore(contentType));
    }
}
