package dev.skymetron.infrastructure.web.rest;

import dev.skymetron.application.agent.*;
import dev.skymetron.domain.execution.AgentMetrics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/agents")
@Tag(name = "Agents", description = "Agent metrics — health, status, request counts per agent")
public class AgentMetricsController {

    private final CeoAgent ceoAgent;
    private final MemoryAgent memoryAgent;
    private final ToolAgent toolAgent;
    private final ResearchAgent researchAgent;
    private final ConsolidationAgent consolidationAgent;

    public AgentMetricsController(CeoAgent ceoAgent, MemoryAgent memoryAgent,
                                  ToolAgent toolAgent, ResearchAgent researchAgent,
                                  ConsolidationAgent consolidationAgent) {
        this.ceoAgent = ceoAgent;
        this.memoryAgent = memoryAgent;
        this.toolAgent = toolAgent;
        this.researchAgent = researchAgent;
        this.consolidationAgent = consolidationAgent;
    }

    @Operation(summary = "All agent metrics", description = "Health, status, request counts, and success rates for all agents")
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> allMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("ceo", toMap("CeoAgent", ceoAgent.metrics(), ceoAgent.health(), ceoAgent.status()));
        metrics.put("memory", toMap("MemoryAgent", memoryAgent.metrics(), memoryAgent.health(), memoryAgent.status()));
        metrics.put("tool", toMap("ToolAgent", toolAgent.metrics(), toolAgent.health(), toolAgent.status()));
        metrics.put("research", toMap("ResearchAgent", researchAgent.metrics(), researchAgent.health(), researchAgent.status()));
        metrics.put("consolidation", toMap("ConsolidationAgent", consolidationAgent.metrics(),
                consolidationAgent.health(), consolidationAgent.status()));
        metrics.put("consolidation_mergedTotal", consolidationAgent.getMergedCount());
        return ResponseEntity.ok(metrics);
    }

    private Map<String, Object> toMap(String name, AgentMetrics m,
                                       dev.skymetron.domain.execution.HealthStatus health,
                                       dev.skymetron.domain.execution.AgentStatus status) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("status", status.name());
        map.put("health", health.name());
        map.put("totalRequests", m.totalRequests());
        map.put("successful", m.successfulRequests());
        map.put("failed", m.failedRequests());
        map.put("averageLatencyMs", m.averageLatencyMs());
        map.put("successRate", m.successRate());
        return map;
    }
}
