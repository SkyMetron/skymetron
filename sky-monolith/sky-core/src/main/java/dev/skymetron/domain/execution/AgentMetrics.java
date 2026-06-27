package dev.skymetron.domain.execution;

/**
 * Metrics snapshot for an agent.
 */
public record AgentMetrics(
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        double averageLatencyMs,
        double successRate
) {
    public static AgentMetrics empty() {
        return new AgentMetrics(0, 0, 0, 0, 0);
    }
}
