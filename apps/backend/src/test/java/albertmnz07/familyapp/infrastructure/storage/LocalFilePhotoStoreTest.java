package albertmnz07.familyapp.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import albertmnz07.familyapp.domain.valueobject.PhotoContentType;
import albertmnz07.familyapp.domain.valueobject.PhotoKey;
import albertmnz07.familyapp.domain.valueobject.RawPhoto;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFilePhotoStoreTest {

    @TempDir Path storageDir;

    @Test
    void putReturnsAKeyWhoseContentCanBeReadBack() throws Exception {
        LocalFilePhotoStore store = new LocalFilePhotoStore(storageDir.toString());

        PhotoKey key =
                store.put(new RawPhoto(new byte[] {1, 2, 3}, PhotoContentType.of("image/png")));

        assertArrayEquals(new byte[] {1, 2, 3}, store.get(key).orElseThrow());
    }

    @Test
    void getReturnsEmptyForAnUnknownKey() throws Exception {
        LocalFilePhotoStore store = new LocalFilePhotoStore(storageDir.toString());

        assertTrue(store.get(new PhotoKey("00000000-0000-0000-0000-000000000000")).isEmpty());
    }

    @Test
    void getReturnsEmptyForAKeyOutsideTheStorageRoot() throws Exception {
        LocalFilePhotoStore store = new LocalFilePhotoStore(storageDir.toString());

        assertTrue(store.get(new PhotoKey("../../etc/passwd")).isEmpty());
        assertTrue(store.get(null).isEmpty());
    }
}
