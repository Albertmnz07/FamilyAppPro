package albertmnz07.familyapp.legacy.family.auth;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 100_000;
    private static final int MAX_ITERATIONS = 1_000_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final String SEPARATOR = ":";
    private static final SecureRandom RANDOM = new SecureRandom();

    public String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(password, salt, ITERATIONS);
        return ITERATIONS + SEPARATOR + hex(salt) + SEPARATOR + hex(derived);
    }

    public boolean matches(String password, String stored) {
        if (stored == null) {
            return false;
        }
        String[] parts = stored.split(SEPARATOR);
        if (parts.length != 3) {
            return false;
        }
        int iterations;
        try {
            iterations = Integer.parseInt(parts[0]);
        } catch (NumberFormatException invalidIterations) {
            return false;
        }
        if (iterations <= 0 || iterations > MAX_ITERATIONS) {
            return false;
        }
        byte[] salt = parseHex(parts[1]);
        byte[] expected = parseHex(parts[2]);
        if (salt == null || expected == null) {
            return false;
        }
        byte[] actual = derive(password, salt, iterations);
        return MessageDigest.isEqual(expected, actual);
    }

    private byte[] derive(String password, byte[] salt, int iterations) {
        try {
            KeySpec spec =
                    new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception cryptoFailure) {
            throw new IllegalStateException("Unable to hash the password", cryptoFailure);
        }
    }

    private String hex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    private byte[] parseHex(String value) {
        try {
            return HexFormat.of().parseHex(value);
        } catch (IllegalArgumentException invalidHex) {
            return null;
        }
    }
}
