package albertmnz07.familyapp.application.port;

import albertmnz07.familyapp.domain.valueobject.PhotoKey;
import albertmnz07.familyapp.domain.valueobject.RawPhoto;
import java.util.Optional;

public interface PhotoStore {

    PhotoKey put(RawPhoto photo);

    Optional<byte[]> get(PhotoKey key);
}
