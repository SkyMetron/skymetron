package dev.skymetron.application.agent;

import dev.skymetron.domain.execution.*;
import dev.skymetron.domain.observation.ToolExecutedEvent;
import dev.skymetron.domain.tool.Tool;
import dev.skymetron.domain.tool.ToolResult;
import dev.skymetron.infrastructure.messaging.EventPublisher;
import dev.skymetron.infrastructure.metrics.SkyMetricsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tool Agent — executes tools (filesystem, git, docker, etc.) on behalf of
 * the CEO.
 *
 * <p>Parses the user's request for tool name and parameters, evaluates the
 * action through {@code AgentSafetyPolicy} (via the Tool adapter itself),
 * and returns the formatted result.
 */
@Component
public class ToolAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(ToolAgent.class);
    private static final AgentId TOOL_ID = AgentId.of("00000000-0000-0000-0000-000000000003");

    private final List<Tool> tools;
    private final Map<String, Tool> toolMap;
    private final EventPublisher eventPublisher;
    private final SkyMetricsRegistry metrics;

    private volatile AgentStatus status = AgentStatus.IDLE;
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successful = new AtomicLong(0);
    private final AtomicLong failed = new AtomicLong(0);
    private double totalLatencyMs = 0;

    public ToolAgent(List<Tool> tools, EventPublisher eventPublisher, SkyMetricsRegistry metrics) {
        this.tools = tools;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
        this.toolMap = new HashMap<>();
        for (Tool t : tools) {
            toolMap.put(t.name(), t);
        }
        log.info("ToolAgent initialized with {} tools: {}", tools.size(), toolMap.keySet());
    }

    @Override
    public AgentId id() { return TOOL_ID; }

    @Override
    public AgentCapabilities capabilities() {
        return new AgentCapabilities(
                Set.of(Intent.TOOL_EXECUTION),
                Set.of(),
                Set.of("tool.execute")
        );
    }

    @Override
    public AgentStatus status() { return status; }

    @Override
    public CompletableFuture<AgentResponse> process(AgentRequest request) {
        status = AgentStatus.PROCESSING;
        totalRequests.incrementAndGet();
        long start = java.lang.System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> params = parseToolRequest(request.payload());
                String toolName = String.valueOf(params.remove("_tool"));

                Tool tool = toolMap.get(toolName);
                if (tool == null) {
                    long elapsed = java.lang.System.currentTimeMillis() - start;
                    failed.incrementAndGet();
                    status = AgentStatus.IDLE;
                    String available = String.join(", ", toolMap.keySet());
                    return AgentResponse.failure(request,
                            "Unknown tool: " + toolName + ". Available: " + available,
                            Duration.ofMillis(elapsed));
                }

                ToolResult result = tool.execute(params);
                long elapsed = java.lang.System.currentTimeMillis() - start;

                String action = String.valueOf(params.getOrDefault("action", "unknown"));
                eventPublisher.publishToolExecuted(ToolExecutedEvent.of(
                        toolName, action, result.success(), elapsed,
                        tool.defaultRisk().name(), java.util.UUID.randomUUID()));
                metrics.recordToolExecution(toolName, action, result.success(), elapsed);

                if (result.success()) {
                    successful.incrementAndGet();
                    totalLatencyMs += elapsed;
                    status = AgentStatus.IDLE;
                    log.info("[ToolAgent] tool={} success in {}ms", toolName, elapsed);
                    return AgentResponse.success(request, result.output(), 0.9, Duration.ofMillis(elapsed));
                } else {
                    failed.incrementAndGet();
                    status = AgentStatus.IDLE;
                    log.warn("[ToolAgent] tool={} failed: {}", toolName, result.error());
                    return AgentResponse.failure(request, result.error(), Duration.ofMillis(elapsed));
                }
            } catch (Exception e) {
                failed.incrementAndGet();
                status = AgentStatus.ERROR;
                long elapsed = java.lang.System.currentTimeMillis() - start;
                log.error("[ToolAgent] error: {}", e.getMessage(), e);
                return AgentResponse.failure(request, "ToolAgent error: " + e.getMessage(),
                        Duration.ofMillis(elapsed));
            }
        });
    }

    /**
     * Parse a tool request string.
     * Format: "toolName|action=X|path=Y|content=Z"
     * Or simple: "filesystem|list|/tmp"
     */
    Map<String, Object> parseToolRequest(String payload) {
        Map<String, Object> params = new HashMap<>();
        if (payload == null || payload.isBlank()) {
            params.put("_tool", "filesystem");
            return params;
        }

        String[] parts = payload.split("\\|", 2);
        params.put("_tool", parts[0].trim().toLowerCase());

        if (parts.length > 1) {
            String rest = parts[1];
            if (rest.contains("=")) {
                for (String kv : rest.split("\\|")) {
                    int eq = kv.indexOf('=');
                    if (eq > 0) {
                        params.put(kv.substring(0, eq).trim(), kv.substring(eq + 1).trim());
                    }
                }
            } else {
                String[] tokens = rest.split("\\|");
                if (tokens.length >= 1) params.put("action", tokens[0].trim());
                if (tokens.length >= 2) params.put("path", tokens[1].trim());
            }
        }
        return params;
    }

    public List<String> availableTools() {
        return toolMap.keySet().stream().sorted().toList();
    }

    @Override
    public HealthStatus health() {
        long total = totalRequests.get();
        if (total == 0) return HealthStatus.HEALTHY;
        double rate = (double) successful.get() / total;
        if (rate > 0.8) return HealthStatus.HEALTHY;
        if (rate > 0.5) return HealthStatus.DEGRADED;
        return HealthStatus.UNHEALTHY;
    }

    @Override
    public AgentMetrics metrics() {
        long total = totalRequests.get();
        double avgLatency = total > 0 ? totalLatencyMs / total : 0;
        double rate = total > 0 ? (double) successful.get() / total : 0;
        return new AgentMetrics(total, successful.get(), failed.get(), avgLatency, rate);
    }
}
