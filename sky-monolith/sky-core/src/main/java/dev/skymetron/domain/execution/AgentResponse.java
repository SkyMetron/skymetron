package dev.skymetron.domain.execution;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Response from an agent.
 */
public record AgentResponse(
        AgentRequest.RequestId requestId,
        AgentId from,
        boolean success,
        String content,
        double confidence,
        List<String> sources,
        Duration duration,
        UUID traceId
) {
    public static AgentResponse success(AgentRequest request, String content, double confidence, Duration duration) {
        return new AgentResponse(request.id(), request.to(), true, content, confidence,
                List.of(), duration, request.context().traceId());
    }

    public static AgentResponse failure(AgentRequest request, String error, Duration duration) {
        return new AgentResponse(request.id(), request.to(), false, error, 0.0,
                List.of(), duration, request.context().traceId());
    }
}
