package dev.skymetron.domain.execution;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Request to an agent.
 */
public record AgentRequest(
        RequestId id,
        AgentId from,
        AgentId to,
        Intent intent,
        String payload,
        ExecutionContext context,
        Duration deadline
) {
    public record RequestId(UUID value) {
        public static RequestId create() { return new RequestId(UUID.randomUUID()); }
    }

    public static AgentRequest simple(AgentId to, Intent intent, String payload) {
        return new AgentRequest(RequestId.create(), null, to, intent, payload,
                ExecutionContext.empty(), Duration.ofSeconds(30));
    }

    public static AgentRequest fromUser(AgentId to, Intent intent, String payload) {
        return new AgentRequest(RequestId.create(), null, to, intent, payload,
                ExecutionContext.empty(), Duration.ofSeconds(30));
    }
}
