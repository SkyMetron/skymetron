package dev.skymetron.infrastructure.web.rest;

import jakarta.validation.constraints.NotBlank;

public record ModeRequest(@NotBlank String mode) {}
