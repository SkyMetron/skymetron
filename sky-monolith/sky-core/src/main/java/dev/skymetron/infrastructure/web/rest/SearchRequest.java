package dev.skymetron.infrastructure.web.rest;

import dev.skymetron.domain.memory.MemoryType;

public record SearchRequest(
        String query,
        MemoryType type,
        Integer limit
) {
    public int effectiveLimit() {
        return limit != null && limit > 0 ? Math.min(limit, 100) : 10;
    }
}
