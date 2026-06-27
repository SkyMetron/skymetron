package dev.skymetron.domain.observation;

import java.util.UUID;

/**
 * Emitted when any agent is invoked (process() called).
 */
public record AgentInvokedEvent(
        UUID eventId,
        java.time.Instant timestamp,
        UUID traceId,
        String agentId,
        String agentName,
        String intent,
        String requestId,
        long durationMs,
        boolean success
) {
    public static final String ROUTING_KEY = "agent.invoked";

    public static AgentInvokedEvent of(String agentId, String agentName, String intent,
                                       String requestId, long durationMs, boolean success, UUID traceId) {
        return new AgentInvokedEvent(UUID.randomUUID(), java.time.Instant.now(), traceId,
                agentId, agentName, intent, requestId, durationMs, success);
    }
}
