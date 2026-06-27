package dev.skymetron.application.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.skymetron.domain.execution.*;
import dev.skymetron.domain.observation.*;
import dev.skymetron.infrastructure.messaging.rabbitmq.RabbitMqConfig;
import dev.skymetron.infrastructure.metrics.SkyMetricsRegistry;
import dev.skymetron.infrastructure.persistence.jpa.TraceEntry;
import dev.skymetron.infrastructure.persistence.jpa.TraceEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Trace Agent — the observability hub.
 *
 * <p>Consumes ALL events from the RabbitMQ trace queue and persists them to
 * the {@code trace_entries} table (Brain Trace). Exposes a timeline API for
 * inspecting what the system did and why.
 *
 * <p>Not a standard "process()" agent — it's event-driven, not request-driven.
 */
@Component
public class TraceAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(TraceAgent.class);
    private static final AgentId TRACE_ID = AgentId.of("00000000-0000-0000-0000-000000000006");

    private final TraceEntryRepository repository;
    private final ObjectMapper objectMapper;
    private final SkyMetricsRegistry metrics;

    private final AtomicLong totalEvents = new AtomicLong(0);
    private final AtomicLong successful = new AtomicLong(0);
    private final AtomicLong failed = new AtomicLong(0);
    private static final int BATCH_SIZE = 50;
    private final java.util.List<TraceEntry> pendingBatch =
            java.util.Collections.synchronizedList(new java.util.ArrayList<>());

    public TraceAgent(TraceEntryRepository repository, ObjectMapper objectMapper, SkyMetricsRegistry metrics) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    @RabbitListener(queues = RabbitMqConfig.TRACE_QUEUE)
    public void onEvent(String json) {
        totalEvents.incrementAndGet();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            String eventType = (String) map.getOrDefault("eventType", "unknown");
            String eventIdStr = String.valueOf(map.get("eventId"));
            String traceIdStr = String.valueOf(map.get("traceId"));
            UUID eventId = parseUuid(eventIdStr);
            UUID traceId = parseUuid(traceIdStr);
            String agentId = extractAgentId(map, eventType);

            TraceEntry entry = new TraceEntry(eventId, eventType, traceId, agentId, map);
            pendingBatch.add(entry);
            if (pendingBatch.size() >= BATCH_SIZE) {
                flushBatch();
            }
            successful.incrementAndGet();
            metrics.recordEventConsumed(eventType, true);
            log.debug("[TraceAgent] stored event: type={} agent={}", eventType, agentId);
        } catch (Exception e) {
            failed.incrementAndGet();
            metrics.recordEventConsumed("unknown", false);
            log.error("[TraceAgent] failed to store event: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 5000)
    synchronized void flushBatch() {
        if (pendingBatch.isEmpty()) return;
        List<TraceEntry> batch = new java.util.ArrayList<>(pendingBatch);
        pendingBatch.clear();
        repository.saveAll(batch);
        log.debug("[TraceAgent] flushed {} events", batch.size());
    }

    private String extractAgentId(Map<String, Object> map, String eventType) {
        return (String) map.getOrDefault("agentId",
                map.getOrDefault("agentName",
                        map.getOrDefault("toolName",
                                map.getOrDefault("failedProvider", "system"))).toString());
    }

    private UUID parseUuid(String s) {
        if (s == null || s.isBlank()) return UUID.randomUUID();
        try { return UUID.fromString(s); } catch (Exception e) { return UUID.randomUUID(); }
    }

    /**
     * Get the latest N trace entries (timeline).
     */
    public Page<TraceEntry> getTimeline(int page, int size) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, Math.min(size, 100)));
    }

    /**
     * Get trace entries for a specific agent.
     * Supports both agentId (UUID) and agentName lookup.
     */
    public Page<TraceEntry> getAgentTrace(String agentId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<TraceEntry> result = repository.findByAgentIdOrderByCreatedAtDesc(agentId, pageable);
        if (result.hasContent()) return result;
        return repository.findByPayloadJsonb("{\"agentName\":\"" + agentId + "\"}", pageable);
    }

    /**
     * Get trace entries for a specific event type.
     */
    public Page<TraceEntry> getByEventType(String eventType, int page, int size) {
        return repository.findByEventTypeOrderByCreatedAtDesc(eventType, PageRequest.of(page, Math.min(size, 100)));
    }

    /**
     * Count events by type.
     */
    @Cacheable(value = "traceCounts", unless = "#result.isEmpty()")
    public Map<String, Long> getCounts() {
        Map<String, Long> counts = new HashMap<>();
        for (String type : Set.of(
                AgentInvokedEvent.ROUTING_KEY, MemoryStoredEvent.ROUTING_KEY,
                MemoryConsolidatedEvent.ROUTING_KEY, ToolExecutedEvent.ROUTING_KEY,
                ResearchCompletedEvent.ROUTING_KEY, ProviderFallbackEvent.ROUTING_KEY)) {
            counts.put(type, repository.countByEventType(type));
        }
        return counts;
    }

    @Override
    public AgentId id() { return TRACE_ID; }

    @Override
    public AgentCapabilities capabilities() {
        return new AgentCapabilities(Set.of(), Set.of(), Set.of("trace.read", "trace.write"));
    }

    @Override
    public AgentStatus status() { return AgentStatus.IDLE; }

    @Override
    public CompletableFuture<AgentResponse> process(AgentRequest request) {
        return CompletableFuture.completedFuture(AgentResponse.success(request,
                "TraceAgent is event-driven. Total events: " + totalEvents.get(),
                1.0, Duration.ZERO));
    }

    @Override
    public AgentMetrics metrics() {
        long total = totalEvents.get();
        double rate = total > 0 ? (double) successful.get() / total : 0;
        return new AgentMetrics(total, successful.get(), failed.get(), 0, rate);
    }

    @CacheEvict(value = "traceCounts", allEntries = true)
    @Scheduled(fixedDelay = 60000)
    public void clearCountsCache() {
    }

    @Override
    public HealthStatus health() {
        long total = totalEvents.get();
        if (total == 0) return HealthStatus.HEALTHY;
        double rate = (double) successful.get() / total;
        if (rate > 0.9) return HealthStatus.HEALTHY;
        if (rate > 0.7) return HealthStatus.DEGRADED;
        return HealthStatus.UNHEALTHY;
    }
}
