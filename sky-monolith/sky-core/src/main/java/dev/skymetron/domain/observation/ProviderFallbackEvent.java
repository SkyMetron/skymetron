package dev.skymetron.domain.observation;

import java.util.UUID;

/**
 * Emitted when the ProviderRegistry falls back to an alternate provider.
 */
public record ProviderFallbackEvent(
        UUID eventId,
        java.time.Instant timestamp,
        UUID traceId,
        String failedProvider,
        String fallbackProvider,
        String reason
) {
    public static final String ROUTING_KEY = "provider.fallback";

    public static ProviderFallbackEvent of(String failedProvider, String fallbackProvider,
                                           String reason, UUID traceId) {
        return new ProviderFallbackEvent(UUID.randomUUID(), java.time.Instant.now(), traceId,
                failedProvider, fallbackProvider, reason);
    }
}
