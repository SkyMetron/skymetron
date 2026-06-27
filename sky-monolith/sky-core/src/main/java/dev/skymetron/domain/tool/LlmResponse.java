package dev.skymetron.domain.tool;

import java.time.Duration;

/**
 * Response from an LLM provider.
 */
public record LlmResponse(
        String content,
        String model,
        String providerId,
        int promptTokens,
        int completionTokens,
        Duration duration,
        boolean fromCache,
        boolean fromFallback
) {
    public LlmResponse withFromFallback(boolean fallback) {
        return new LlmResponse(content, model, providerId, promptTokens, completionTokens,
                duration, fromCache, fallback);
    }
}
