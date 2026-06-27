package dev.skymetron.domain.observation;

import java.time.Instant;
import java.util.UUID;

public record SystemOverloadEvent(
        UUID eventId,
        Instant timestamp,
        String reason
) {
    public static final String ROUTING_KEY = "system.overload";

    public static SystemOverloadEvent of(String reason) {
        return new SystemOverloadEvent(UUID.randomUUID(), Instant.now(), reason);
    }
}
