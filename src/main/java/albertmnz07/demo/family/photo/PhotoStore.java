package albertmnz07.demo.family.photo;

import java.util.Optional;

public interface PhotoStore {

    String put(byte[] content);

    Optional<byte[]> get(String key);
}
