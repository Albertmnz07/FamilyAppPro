package albertmnz07.familyapp.domain.valueobject;

import java.util.Objects;
import java.util.Optional;

/** A persona's DNI: the required number plus the optional front and back photos. */
public final class Dni {

    private final DniNumber number;
    private final DniPhoto front;
    private final DniPhoto back;

    public Dni(DniNumber number, DniPhoto front, DniPhoto back) {
        this.number = Objects.requireNonNull(number, "number");
        this.front = front;
        this.back = back;
    }

    public static Dni withoutPhotos(DniNumber number) {
        return new Dni(number, null, null);
    }

    public DniNumber number() {
        return number;
    }

    public Optional<DniPhoto> front() {
        return Optional.ofNullable(front);
    }

    public Optional<DniPhoto> back() {
        return Optional.ofNullable(back);
    }

    /** Replaces the given sides; a {@code null} side keeps the current photo. */
    public Dni withPhotos(DniPhoto newFront, DniPhoto newBack) {
        return new Dni(
                number, newFront != null ? newFront : front, newBack != null ? newBack : back);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Dni dni
                && number.equals(dni.number)
                && Objects.equals(front, dni.front)
                && Objects.equals(back, dni.back);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, front, back);
    }
}
