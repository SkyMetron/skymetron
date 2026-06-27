package dev.skymetron.infrastructure.web.rest;

import dev.skymetron.domain.memory.MemoryEntry;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MemoryResponse(
        UUID id,
        String content,
        String source,
        double confidence,
        UUID mergedInto,
        Map<String, Object> metadata,
        Instant createdAt,
        Instant updatedAt
) {
    public static MemoryResponse from(MemoryEntry entry) {
        return new MemoryResponse(
                entry.getId(),
                entry.getContent(),
                entry.getSource(),
                entry.getConfidence(),
                entry.getMergedInto(),
                entry.getMetadata(),
                entry.getCreatedAt(),
                entry.getUpdatedAt());
    }
}
