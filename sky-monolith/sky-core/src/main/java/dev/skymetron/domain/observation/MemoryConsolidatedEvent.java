package dev.skymetron.domain.observation;

import java.util.UUID;

/**
 * Emitted when the ConsolidationAgent merges duplicate memory entries.
 */
public record MemoryConsolidatedEvent(
        UUID eventId,
        java.time.Instant timestamp,
        UUID traceId,
        UUID mergedEntryId,
        UUID targetEntryId,
        double similarity
) {
    public static final String ROUTING_KEY = "memory.consolidated";

    public static MemoryConsolidatedEvent of(UUID mergedEntryId, UUID targetEntryId,
                                             double similarity, UUID traceId) {
        return new MemoryConsolidatedEvent(UUID.randomUUID(), java.time.Instant.now(), traceId,
                mergedEntryId, targetEntryId, similarity);
    }
}
