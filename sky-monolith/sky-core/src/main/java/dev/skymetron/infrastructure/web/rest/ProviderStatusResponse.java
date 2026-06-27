package dev.skymetron.infrastructure.web.rest;

public record ProviderStatusResponse(
        String providerId,
        boolean available,
        boolean free,
        String lastError
) {
}
