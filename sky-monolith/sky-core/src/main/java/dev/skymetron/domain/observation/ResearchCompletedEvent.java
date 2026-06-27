package dev.skymetron.domain.observation;

import java.util.UUID;

/**
 * Emitted when the ResearchAgent completes a research mission.
 */
public record ResearchCompletedEvent(
        UUID eventId,
        java.time.Instant timestamp,
        UUID traceId,
        String query,
        UUID storedMemoryId,
        long durationMs,
        int resultsCount
) {
    public static final String ROUTING_KEY = "research.completed";

    public static ResearchCompletedEvent of(String query, UUID storedMemoryId,
                                            long durationMs, int resultsCount, UUID traceId) {
        return new ResearchCompletedEvent(UUID.randomUUID(), java.time.Instant.now(), traceId,
                query, storedMemoryId, durationMs, resultsCount);
    }
}
