package albertmnz07.familyapp.infrastructure.security;

import albertmnz07.familyapp.application.port.Clock;
import java.time.Instant;

public final class SystemClock implements Clock {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
