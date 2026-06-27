package dev.skymetron.domain.observation;

import java.time.Instant;
import java.util.UUID;

public record SystemRecoveryEvent(
        UUID eventId,
        Instant timestamp,
        String reason
) {
    public static final String ROUTING_KEY = "system.recovery";

    public static SystemRecoveryEvent of(String reason) {
        return new SystemRecoveryEvent(UUID.randomUUID(), Instant.now(), reason);
    }
}
