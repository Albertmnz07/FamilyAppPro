package albertmnz07.familyapp.application.port;

import albertmnz07.familyapp.domain.valueobject.PasswordHash;
import albertmnz07.familyapp.domain.valueobject.RawPassword;

public interface PasswordHasher {

    PasswordHash hash(RawPassword password);

    boolean matches(RawPassword password, PasswordHash hash);
}
