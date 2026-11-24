package albertmnz07.familyapp.domain.model;

import albertmnz07.familyapp.domain.exception.ForbiddenException;
import albertmnz07.familyapp.domain.valueobject.FamilyId;
import albertmnz07.familyapp.domain.valueobject.FamilyName;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import java.util.Objects;

/** Aggregate Root: a named grouping of personas administered by its creator. */
public final class Family {

    private final FamilyId id;
    private final FamilyName name;
    private final PersonaId adminId;

    private Family(FamilyId id, FamilyName name, PersonaId adminId) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.adminId = Objects.requireNonNull(adminId, "adminId");
    }

    public static Family create(FamilyName name, PersonaId adminId) {
        return new Family(FamilyId.generate(), name, adminId);
    }

    public static Family restore(FamilyId id, FamilyName name, PersonaId adminId) {
        return new Family(id, name, adminId);
    }

    public boolean isAdmin(PersonaId personaId) {
        Objects.requireNonNull(personaId, "personaId");
        return adminId.equals(personaId);
    }

    /** Refuses any non-admin actor with {@code forbidden}. */
    public void requireAdmin(PersonaId personaId) {
        if (!isAdmin(personaId)) {
            throw new ForbiddenException("Only the family admin can perform this action");
        }
    }

    public FamilyId id() {
        return id;
    }

    public FamilyName name() {
        return name;
    }

    public PersonaId adminId() {
        return adminId;
    }
}
