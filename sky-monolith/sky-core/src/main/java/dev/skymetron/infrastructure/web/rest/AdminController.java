package dev.skymetron.infrastructure.web.rest;

import dev.skymetron.domain.execution.Agent;
import dev.skymetron.domain.execution.AgentId;
import dev.skymetron.domain.execution.AgentRequest;
import dev.skymetron.domain.execution.Intent;
import dev.skymetron.infrastructure.audit.AuditLog;
import dev.skymetron.infrastructure.metrics.SkyMetricsRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "System administration — benchmark, memory, thread diagnostics")
public class AdminController {

    private final List<Agent> agents;
    private final SkyMetricsRegistry metrics;

    public AdminController(List<Agent> agents, SkyMetricsRegistry metrics) {
        this.agents = agents;
        this.metrics = metrics;
    }

    @AuditLog(action = "admin.benchmark")
    @GetMapping("/benchmark")
    @Operation(summary = "Run benchmark", description = "Pings all agents and measures p95 latency in ms")
    public ResponseEntity<Map<String, Object>> benchmark() {
        Map<String, Object> results = new LinkedHashMap<>();
        results.put("timestamp", System.currentTimeMillis());

        List<Map<String, Object>> agentLatencies = new ArrayList<>();
        for (Agent agent : agents) {
            long start = System.nanoTime();
            try {
                AgentRequest ping = AgentRequest.simple(agent.id(), Intent.GENERAL_CHAT, "ping");
                agent.process(ping).get(5, TimeUnit.SECONDS);
                long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("agent", agent.id().value());
                entry.put("latencyMs", elapsedMs);
                entry.put("status", "ok");
                agentLatencies.add(entry);
            } catch (Exception e) {
                long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("agent", agent.id().value());
                entry.put("latencyMs", elapsedMs);
                entry.put("status", "error");
                entry.put("error", e.getMessage());
                agentLatencies.add(entry);
            }
        }
        results.put("agents", agentLatencies);

        double p95 = computeP95(agentLatencies);
        results.put("p95Ms", p95);
        results.put("status", p95 > 3000 ? "degraded" : "healthy");

        return ResponseEntity.ok(results);
    }

    @GetMapping("/memory")
    @Operation(summary = "JVM memory usage", description = "Heap and non-heap memory in bytes")
    public ResponseEntity<Map<String, Object>> memory() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("heap", Map.of(
                "init", heap.getInit(),
                "used", heap.getUsed(),
                "committed", heap.getCommitted(),
                "max", heap.getMax(),
                "usedPercent", heap.getMax() > 0
                        ? Math.round((double) heap.getUsed() / heap.getMax() * 100) : 0
        ));
        info.put("nonHeap", Map.of(
                "used", nonHeap.getUsed(),
                "committed", nonHeap.getCommitted()
        ));
        return ResponseEntity.ok(info);
    }

    @GetMapping("/threads")
    @Operation(summary = "Thread diagnostics", description = "Active, daemon, peak, and total started threads")
    public ResponseEntity<Map<String, Object>> threads() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("active", threadBean.getThreadCount());
        info.put("daemon", threadBean.getDaemonThreadCount());
        info.put("peak", threadBean.getPeakThreadCount());
        info.put("totalStarted", threadBean.getTotalStartedThreadCount());
        return ResponseEntity.ok(info);
    }

    private double computeP95(List<Map<String, Object>> latencies) {
        List<Long> values = latencies.stream()
                .map(m -> (Long) m.get("latencyMs"))
                .sorted()
                .toList();
        if (values.isEmpty()) return 0;
        int index = (int) Math.ceil(0.95 * values.size()) - 1;
        return values.get(Math.max(0, index));
    }
}
