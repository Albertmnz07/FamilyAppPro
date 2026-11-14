package albertmnz07.demo.family;

import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class FamilyService {

    static final Duration INVITE_TTL = Duration.ofDays(7);
    private static final int MAX_CODE_ATTEMPTS = 10;

    private final FamilyRepository families;
    private final InviteRepository invites;
    private final InviteCodeGenerator codeGenerator;

    public FamilyService(FamilyRepository families, InviteRepository invites, InviteCodeGenerator codeGenerator) {
        this.families = families;
        this.invites = invites;
        this.codeGenerator = codeGenerator;
    }

    public Family createFamily(Persona creator, String name) {
        return families.save(new Family(name, creator));
    }

    public Family getFamily(UUID id) {
        return families.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Family not found: " + id));
    }

    public Invite issueInvite(Family family) {
        Instant issuedAt = Instant.now();
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = codeGenerator.generate();
            if (!invites.existsByCode(code)) {
                return invites.save(new Invite(code, family, issuedAt.plus(INVITE_TTL)));
            }
        }
        throw new IllegalStateException("Could not generate a unique invite code");
    }
}
