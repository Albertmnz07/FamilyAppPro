package albertmnz07.familyapp.infrastructure.security;

import albertmnz07.familyapp.application.port.InviteCodeGenerator;
import albertmnz07.familyapp.domain.valueobject.InviteCode;
import java.security.SecureRandom;

/** Emits unambiguous 6-character alphanumeric invite codes (omitting 0, O, 1, I). */
public class SecureRandomInviteCodeGenerator implements InviteCodeGenerator {

    static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final SecureRandom random = new SecureRandom();

    @Override
    public InviteCode generate() {
        StringBuilder code = new StringBuilder(InviteCode.LENGTH);
        for (int i = 0; i < InviteCode.LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return InviteCode.of(code.toString());
    }
}
