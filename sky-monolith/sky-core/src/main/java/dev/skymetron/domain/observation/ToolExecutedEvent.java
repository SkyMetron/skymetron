package dev.skymetron.domain.observation;

import java.util.UUID;

/**
 * Emitted when a tool is executed by the ToolAgent.
 */
public record ToolExecutedEvent(
        UUID eventId,
        java.time.Instant timestamp,
        UUID traceId,
        String toolName,
        String action,
        boolean success,
        long durationMs,
        String riskLevel
) {
    public static final String ROUTING_KEY = "tool.executed";

    public static ToolExecutedEvent of(String toolName, String action, boolean success,
                                       long durationMs, String riskLevel, UUID traceId) {
        return new ToolExecutedEvent(UUID.randomUUID(), java.time.Instant.now(), traceId,
                toolName, action, success, durationMs, riskLevel);
    }
}
