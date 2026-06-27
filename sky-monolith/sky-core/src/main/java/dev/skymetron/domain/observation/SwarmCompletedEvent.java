package dev.skymetron.domain.observation;

import java.util.UUID;

public record SwarmCompletedEvent(
        UUID eventId,
        java.time.Instant timestamp,
        UUID traceId,
        String query,
        UUID storedMemoryId,
        long durationMs,
        int totalWorkers,
        long successfulWorkers
) {
    public static final String ROUTING_KEY = "swarm.completed";

    public static SwarmCompletedEvent of(String query, UUID storedMemoryId,
                                         long durationMs, int totalWorkers,
                                         long successfulWorkers, UUID traceId) {
        return new SwarmCompletedEvent(UUID.randomUUID(), java.time.Instant.now(), traceId,
                query, storedMemoryId, durationMs, totalWorkers, successfulWorkers);
    }
}
