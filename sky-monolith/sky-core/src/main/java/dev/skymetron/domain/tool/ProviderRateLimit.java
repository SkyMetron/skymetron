package dev.skymetron.domain.tool;

import java.time.Duration;

/**
 * Rate limit metadata for a provider's free tier.
 */
public record ProviderRateLimit(
        int requestsPerMinute,
        Duration cooldownAfterLimit
) {
    public static ProviderRateLimit of(int rpm) {
        return new ProviderRateLimit(rpm, Duration.ofSeconds(60));
    }
}
