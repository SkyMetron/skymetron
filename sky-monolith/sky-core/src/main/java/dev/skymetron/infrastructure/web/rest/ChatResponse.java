package dev.skymetron.infrastructure.web.rest;

public record ChatResponse(
        String content,
        String providerId,
        boolean fromFallback,
        String intent,
        String taskType,
        String routedAgent,
        long durationMs,
        int promptTokens,
        int completionTokens
) {
}
