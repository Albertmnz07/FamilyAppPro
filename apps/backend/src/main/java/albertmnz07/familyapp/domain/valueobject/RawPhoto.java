package albertmnz07.familyapp.domain.valueobject;

import albertmnz07.familyapp.domain.exception.InvalidValueException;
import albertmnz07.familyapp.domain.exception.PhotoTooLargeException;
import java.util.Objects;

/** An uploaded, not yet stored DNI photo: at most 10 MB of image/jpeg or image/png. */
public record RawPhoto(byte[] content, PhotoContentType contentType) {

    public static final int MAX_BYTES = 10 * 1024 * 1024;

    public RawPhoto {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(contentType, "contentType");
        if (content.length == 0) {
            throw new InvalidValueException("The photo is required");
        }
        if (content.length > MAX_BYTES) {
            throw new PhotoTooLargeException("The photo exceeds 10 MB");
        }
    }

    /** Validates an upload (presence, size, then type), naming the side in refusal messages. */
    public static RawPhoto of(PhotoSide side, byte[] content, String contentType) {
        Objects.requireNonNull(side, "side");
        if (content == null || content.length == 0) {
            throw new InvalidValueException("The " + side.lowerName() + " photo is required");
        }
        if (content.length > MAX_BYTES) {
            throw new PhotoTooLargeException("The " + side.lowerName() + " photo exceeds 10 MB");
        }
        return new RawPhoto(content, PhotoContentType.of(contentType, side));
    }
}
