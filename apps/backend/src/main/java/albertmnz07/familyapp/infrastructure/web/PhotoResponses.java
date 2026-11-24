package albertmnz07.familyapp.infrastructure.web;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public final class PhotoResponses {

    private PhotoResponses() {}

    public static ResponseEntity<byte[]> of(byte[] photo, String contentType) {
        return ResponseEntity.ok()
                .contentType(mediaType(contentType))
                .cacheControl(CacheControl.noStore())
                .body(photo);
    }

    private static MediaType mediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException | NullPointerException unparseable) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
