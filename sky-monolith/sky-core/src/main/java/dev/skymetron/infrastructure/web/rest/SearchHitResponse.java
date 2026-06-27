package dev.skymetron.infrastructure.web.rest;

import java.util.UUID;

public record SearchHitResponse(
        UUID id,
        String content,
        String source,
        double confidence,
        double similarity
) {
}
