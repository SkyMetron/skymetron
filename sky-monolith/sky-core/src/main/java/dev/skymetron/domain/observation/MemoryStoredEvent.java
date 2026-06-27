package dev.skymetron.domain.observation;

import java.util.UUID;

/**
 * Emitted when a memory entry is stored in the Vault.
 */
public record MemoryStoredEvent(
        UUID eventId,
        java.time.Instant timestamp,
        UUID traceId,
        UUID memoryId,
        String memoryType,
        String source,
        String contentPreview
) {
    public static final String ROUTING_KEY = "memory.stored";

    public static MemoryStoredEvent of(UUID memoryId, String memoryType, String source,
                                       String contentPreview, UUID traceId) {
        return new MemoryStoredEvent(UUID.randomUUID(), java.time.Instant.now(), traceId,
                memoryId, memoryType, source, contentPreview);
    }
}
