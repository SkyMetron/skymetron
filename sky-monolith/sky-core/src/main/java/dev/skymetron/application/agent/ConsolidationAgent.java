package dev.skymetron.application.agent;

import dev.skymetron.application.provider.ProviderRegistry;
import dev.skymetron.application.usecase.MemoryService;
import dev.skymetron.domain.execution.*;
import dev.skymetron.domain.observation.MemoryConsolidatedEvent;
import dev.skymetron.domain.tool.*;
import dev.skymetron.infrastructure.messaging.EventPublisher;
import dev.skymetron.infrastructure.metrics.SkyMetricsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Consolidation Agent — deduplicates and consolidates the Vault.
 *
 * <p>Periodically scans active memory entries, finds near-duplicates
 * (cosine distance < 0.05, i.e. similarity > 0.95), validates with an LLM
 * whether they are truly duplicates, and merges them.
 *
 * <p>Runs via LoopScheduler (4h frequency, 30s timeout, max 3 failures).
 */
@Component
public class ConsolidationAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(ConsolidationAgent.class);
    private static final AgentId CONSOLIDATION_ID = AgentId.of("00000000-0000-0000-0000-000000000004");
    private static final double DEDUP_THRESHOLD = 0.05;
    private static final int BATCH_SIZE = 50;

    private final MemoryService memoryService;
    private final ProviderRegistry providerRegistry;
    private final EventPublisher eventPublisher;
    private final SkyMetricsRegistry metrics;

    private volatile AgentStatus status = AgentStatus.IDLE;
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successful = new AtomicLong(0);
    private final AtomicLong failed = new AtomicLong(0);
    private final AtomicLong mergedCount = new AtomicLong(0);
    private double totalLatencyMs = 0;

    public ConsolidationAgent(MemoryService memoryService, ProviderRegistry providerRegistry,
                              EventPublisher eventPublisher, SkyMetricsRegistry metrics) {
        this.memoryService = memoryService;
        this.providerRegistry = providerRegistry;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
    }

    @Override
    public AgentId id() { return CONSOLIDATION_ID; }

    @Override
    public AgentCapabilities capabilities() {
        return new AgentCapabilities(
                Set.of(),
                Set.of(TaskType.MEMORY_DEDUP, TaskType.VAULT_CONSOLIDATION),
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
                String result = runConsolidation();
                long elapsed = java.lang.System.currentTimeMillis() - start;
                successful.incrementAndGet();
                totalLatencyMs += elapsed;
                status = AgentStatus.IDLE;
                return AgentResponse.success(request, result, 0.8, Duration.ofMillis(elapsed));
            } catch (Exception e) {
                failed.incrementAndGet();
                status = AgentStatus.ERROR;
                long elapsed = java.lang.System.currentTimeMillis() - start;
                log.error("[ConsolidationAgent] failed: {}", e.getMessage(), e);
                return AgentResponse.failure(request, "Consolidation error: " + e.getMessage(),
                        Duration.ofMillis(elapsed));
            }
        });
    }

    /**
     * Scan the Vault for duplicates and merge them.
     *
     * @return summary of what was consolidated
     */
    public String runConsolidation() {
        log.info("[ConsolidationAgent] starting Vault scan");
        int scanned = 0;
        int merged = 0;
        int skipped = 0;

        var page = memoryService.listActive(null, PageRequest.of(0, BATCH_SIZE));
        for (var entry : page.getContent()) {
            scanned++;
            if (entry.getEmbedding() == null) continue;

            String literal = dev.skymetron.infrastructure.ai.ollama.EmbeddingClient.toVectorLiteral(entry.getEmbedding());
            List<UUID> dupes = memoryService.findDuplicatesRaw(literal, DEDUP_THRESHOLD, entry.getId(), 5);

            if (dupes.isEmpty()) continue;

            for (UUID dupeId : dupes) {
                var dupeOpt = memoryService.getById(dupeId);
                if (dupeOpt.isEmpty() || dupeOpt.get().getMergedInto() != null) continue;

                boolean shouldMerge = validateMergeWithLlm(entry.getContent(), dupeOpt.get().getContent());
                if (shouldMerge) {
                    memoryService.markMerged(dupeId, entry.getId());
                    merged++;
                    mergedCount.incrementAndGet();
                    eventPublisher.publishMemoryConsolidated(MemoryConsolidatedEvent.of(
                            dupeId, entry.getId(), 1.0 - DEDUP_THRESHOLD, java.util.UUID.randomUUID()));
                    metrics.recordMemoryMerged();
                    log.info("[ConsolidationAgent] merged {} into {} (Brain Trace)", dupeId, entry.getId());
                } else {
                    skipped++;
                    log.debug("[ConsolidationAgent] LLM said NOT duplicates: {} vs {}", entry.getId(), dupeId);
                }
            }
        }

        String summary = String.format("Consolidation complete: scanned=%d merged=%d skipped=%d",
                scanned, merged, skipped);
        log.info("[ConsolidationAgent] {}", summary);
        metrics.setMemoryCount(memoryService.countActive());
        return summary;
    }

    /**
     * Ask an LLM whether two memory entries are truly duplicates.
     * Uses a fast free provider (Groq/Cerebras → Ollama fallback).
     */
    private boolean validateMergeWithLlm(String content1, String content2) {
        String prompt = String.format("""
                Are these two texts semantically duplicate (same information)?
                Answer only YES or NO.

                Text 1: %s

                Text 2: %s
                """, truncate(content1, 500), truncate(content2, 500));

        try {
            LlmRequest request = LlmRequest.builder()
                    .messages(List.of(
                            Message.system("You are a deduplication classifier. Answer YES or NO only."),
                            Message.user(prompt)
                    ))
                    .temperature(0.0)
                    .maxTokens(5)
                    .build();

            LlmResponse response = providerRegistry.chat(request, TaskType.MEMORY_DEDUP).join();
            String answer = response.content().trim().toUpperCase();
            return answer.startsWith("YES");
        } catch (Exception e) {
            log.warn("[ConsolidationAgent] LLM validation failed, skipping merge: {}", e.getMessage());
            return false;
        }
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    public long getMergedCount() { return mergedCount.get(); }

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
