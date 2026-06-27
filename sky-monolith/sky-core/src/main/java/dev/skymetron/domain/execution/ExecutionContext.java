package dev.skymetron.domain.execution;

import java.util.Map;
import java.util.UUID;

/**
 * Context carried through an agent execution — session id, user id, trace id, metadata.
 */
public record ExecutionContext(
        UUID sessionId,
        UUID userId,
        UUID traceId,
        Map<String, Object> metadata
) {
    public static ExecutionContext empty() {
        return new ExecutionContext(null, null, UUID.randomUUID(), Map.of());
    }

    public ExecutionContext withTraceId(UUID traceId) {
        return new ExecutionContext(sessionId, userId, traceId, metadata);
    }
}
