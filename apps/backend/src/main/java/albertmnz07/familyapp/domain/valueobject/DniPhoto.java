package albertmnz07.familyapp.domain.valueobject;

import java.util.Objects;

/** A stored DNI photo: where it lives and what it is. */
public record DniPhoto(PhotoKey key, PhotoContentType contentType) {

    public DniPhoto {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(contentType, "contentType");
    }
}
