package albertmnz07.demo.family.photo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFilePhotoStoreTest {

    @TempDir
    Path storageDir;

    @Test
    void putReturnsAKeyWhoseContentCanBeReadBack() throws Exception {
        LocalFilePhotoStore store = new LocalFilePhotoStore(storageDir.toString());

        String key = store.put(new byte[]{1, 2, 3});

        assertArrayEquals(new byte[]{1, 2, 3}, store.get(key).orElseThrow());
    }

    @Test
    void getReturnsEmptyForAnUnknownKey() throws Exception {
        LocalFilePhotoStore store = new LocalFilePhotoStore(storageDir.toString());

        assertTrue(store.get("00000000-0000-0000-0000-000000000000").isEmpty());
    }

    @Test
    void getReturnsEmptyForAKeyOutsideTheStorageRoot() throws Exception {
        LocalFilePhotoStore store = new LocalFilePhotoStore(storageDir.toString());

        assertTrue(store.get("../../etc/passwd").isEmpty());
        assertTrue(store.get(null).isEmpty());
    }
}
