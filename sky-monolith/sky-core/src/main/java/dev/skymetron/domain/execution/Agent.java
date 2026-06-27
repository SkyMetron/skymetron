package dev.skymetron.domain.execution;

import java.util.concurrent.CompletableFuture;

/**
 * Contract that every agent in SkyMetron must fulfill.
 *
 * <p>No agent may be implemented without this contract (Rule 3 — Absolute Rules).
 * Each agent has a stable id, declared capabilities, a process method, and
 * health/metrics reporting.
 */
public interface Agent {

    AgentId id();

    AgentCapabilities capabilities();

    AgentStatus status();

    CompletableFuture<AgentResponse> process(AgentRequest request);

    HealthStatus health();

    AgentMetrics metrics();
}
