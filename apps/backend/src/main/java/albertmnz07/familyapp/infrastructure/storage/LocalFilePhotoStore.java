package albertmnz07.familyapp.infrastructure.storage;

import albertmnz07.familyapp.application.port.PhotoStore;
import albertmnz07.familyapp.domain.valueobject.PhotoKey;
import albertmnz07.familyapp.domain.valueobject.RawPhoto;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalFilePhotoStore implements PhotoStore {

    private static final Pattern KEY_PATTERN = Pattern.compile("[0-9a-fA-F-]{36}");

    private final Path root;

    public LocalFilePhotoStore(@Value("${app.photos.storage-dir:./data/photos}") String storageDir)
            throws IOException {
        this.root = Path.of(storageDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Override
    public PhotoKey put(RawPhoto photo) {
        String key = UUID.randomUUID().toString();
        try {
            Files.write(
                    root.resolve(key),
                    photo.content(),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
        } catch (IOException storeFailure) {
            throw new IllegalStateException("Unable to store the photo", storeFailure);
        }
        return new PhotoKey(key);
    }

    @Override
    public Optional<byte[]> get(PhotoKey key) {
        if (key == null || !KEY_PATTERN.matcher(key.value()).matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(root.resolve(key.value())));
        } catch (IOException missingPhoto) {
            return Optional.empty();
        }
    }
}
