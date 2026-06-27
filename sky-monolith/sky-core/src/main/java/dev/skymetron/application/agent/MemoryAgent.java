package dev.skymetron.application.agent;

import dev.skymetron.application.usecase.MemoryService;
import dev.skymetron.domain.execution.*;
import dev.skymetron.domain.memory.MemoryType;
import dev.skymetron.domain.observation.MemoryStoredEvent;
import dev.skymetron.infrastructure.messaging.EventPublisher;
import dev.skymetron.infrastructure.metrics.SkyMetricsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Memory Agent — reads and writes the Vault.
 *
 * <p>When the CEO detects a MEMORY_QUERY intent, it delegates to this agent.
 * The agent performs semantic search on the Vault and returns formatted results.
 * For MEMORY_STORE intent, it saves new facts with embeddings.
 */
@Component
public class MemoryAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(MemoryAgent.class);
    private static final AgentId MEMORY_ID = AgentId.of("00000000-0000-0000-0000-000000000002");

    private final MemoryService memoryService;
    private final EventPublisher eventPublisher;
    private final SkyMetricsRegistry metrics;

    private volatile AgentStatus status = AgentStatus.IDLE;
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successful = new AtomicLong(0);
    private final AtomicLong failed = new AtomicLong(0);
    private double totalLatencyMs = 0;

    public MemoryAgent(MemoryService memoryService, EventPublisher eventPublisher, SkyMetricsRegistry metrics) {
        this.memoryService = memoryService;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
    }

    @Override
    public AgentId id() { return MEMORY_ID; }

    @Override
    public AgentCapabilities capabilities() {
        return new AgentCapabilities(
                Set.of(Intent.MEMORY_QUERY, Intent.MEMORY_STORE),
                Set.of(),
                Set.of("memory.read", "memory.write")
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
                String result = switch (request.intent()) {
                    case MEMORY_QUERY -> handleQuery(request.payload());
                    case MEMORY_STORE -> handleStore(request.payload());
                    default -> handleQuery(request.payload());
                };

                long elapsed = java.lang.System.currentTimeMillis() - start;
                successful.incrementAndGet();
                totalLatencyMs += elapsed;
                status = AgentStatus.IDLE;
                log.info("[MemoryAgent] intent={} completed in {}ms", request.intent(), elapsed);
                return AgentResponse.success(request, result, 0.85, Duration.ofMillis(elapsed));
            } catch (Exception e) {
                failed.incrementAndGet();
                status = AgentStatus.ERROR;
                long elapsed = java.lang.System.currentTimeMillis() - start;
                log.error("[MemoryAgent] failed: {}", e.getMessage(), e);
                return AgentResponse.failure(request, "MemoryAgent error: " + e.getMessage(),
                        Duration.ofMillis(elapsed));
            }
        });
    }

    /**
     * Search the Vault for relevant memories.
     */
    private String handleQuery(String query) {
        var hits = memoryService.search(query, null, 5);
        if (hits.isEmpty()) {
            return "No memories found for: " + query;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(hits.size()).append(" memories:\n\n");
        for (int i = 0; i < hits.size(); i++) {
            var hit = hits.get(i);
            sb.append(i + 1).append(". [similarity=").append(String.format("%.2f", hit.similarity()))
              .append("] ").append(hit.content(), 0, Math.min(200, hit.content().length()))
              .append("\n\n");
        }
        return sb.toString();
    }

    /**
     * Store a new fact in the Vault.
     * Expects payload format: "TYPE|content" where TYPE is user_facts, project_knowledge, etc.
     * If no type prefix, defaults to USER_FACTS.
     */
    private String handleStore(String payload) {
        MemoryType type = MemoryType.USER_FACTS;
        String content = payload;

        int pipeIdx = payload.indexOf('|');
        if (pipeIdx > 0 && pipeIdx < 30) {
            String typeStr = payload.substring(0, pipeIdx).trim().toUpperCase();
            try {
                type = MemoryType.valueOf(typeStr);
                content = payload.substring(pipeIdx + 1).trim();
            } catch (IllegalArgumentException ignored) {
            }
        }

        var entry = memoryService.save(content, type, "user");
        log.info("[MemoryAgent] stored fact id={} type={}", entry.getId(), type);
        eventPublisher.publishMemoryStored(MemoryStoredEvent.of(
                entry.getId(), type.name().toLowerCase(), "user",
                content.substring(0, Math.min(100, content.length())),
                java.util.UUID.randomUUID()));
        metrics.recordMemoryStored(type.name().toLowerCase());
        return "Memory stored (id=" + entry.getId() + ", type=" + type.name().toLowerCase() + ")";
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
