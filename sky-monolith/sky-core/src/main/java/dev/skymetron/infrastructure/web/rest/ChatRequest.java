package dev.skymetron.infrastructure.web.rest;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank String message,
        String intent
) {
}
