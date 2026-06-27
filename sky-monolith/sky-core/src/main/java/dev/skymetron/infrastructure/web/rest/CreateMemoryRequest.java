package dev.skymetron.infrastructure.web.rest;

import dev.skymetron.domain.memory.MemoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateMemoryRequest(
        @NotBlank String content,
        @NotNull MemoryType type,
        String source,
        Map<String, Object> metadata
) {
}
