package dev.skymetron.domain.knowledge;

import java.time.Instant;
import java.util.UUID;

public record Skill(
        UUID id,
        String name,
        String description,
        String category,
        String content,
        int version,
        double confidence,
        Instant createdAt,
        Instant updatedAt
) {
    public Skill withVersion(int newVersion) {
        return new Skill(id, name, description, category, content, newVersion, confidence, createdAt, Instant.now());
    }

    public Skill withConfidence(double newConfidence) {
        return new Skill(id, name, description, category, content, version, newConfidence, createdAt, Instant.now());
    }

    public static Skill create(String name, String description, String category, String content) {
        return new Skill(UUID.randomUUID(), name, description, category, content, 1, 0.5, Instant.now(), Instant.now());
    }
}
