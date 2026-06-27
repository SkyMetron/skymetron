package dev.skymetron.application.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.skymetron.application.provider.ProviderRegistry;
import dev.skymetron.application.usecase.MemoryService;
import dev.skymetron.domain.execution.*;
import dev.skymetron.domain.memory.MemoryType;
import dev.skymetron.domain.observation.SwarmCompletedEvent;
import dev.skymetron.domain.tool.*;
import dev.skymetron.infrastructure.messaging.EventPublisher;
import dev.skymetron.infrastructure.metrics.SkyMetricsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.System;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class ResearchSwarmAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(ResearchSwarmAgent.class);
    private static final AgentId SWARM_ID = AgentId.of("00000000-0000-0000-0000-000000000010");
    private static final int MAX_WORKERS = 5;
    private static final int MAX_TOKENS_PER_SWARM = 2048;

    private final ProviderRegistry providerRegistry;
    private final MemoryService memoryService;
    private final EventPublisher eventPublisher;
    private final SkyMetricsRegistry metrics;
    private final Map<String, ResearchWorker> workerMap;
    private final ObjectMapper objectMapper;

    private volatile AgentStatus status = AgentStatus.IDLE;
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successful = new AtomicLong(0);
    private final AtomicLong failed = new AtomicLong(0);
    private double totalLatencyMs = 0;

    public ResearchSwarmAgent(ProviderRegistry providerRegistry, MemoryService memoryService,
                              EventPublisher eventPublisher, SkyMetricsRegistry metrics,
                              List<ResearchWorker> workers, ObjectMapper objectMapper) {
        this.providerRegistry = providerRegistry;
        this.memoryService = memoryService;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
        this.workerMap = workers.stream()
                .collect(Collectors.toMap(ResearchWorker::type, w -> w));
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentId id() { return SWARM_ID; }

    @Override
    public AgentCapabilities capabilities() {
        return new AgentCapabilities(
                Set.of(Intent.RESEARCH),
                Set.of(TaskType.WEB_RESEARCH, TaskType.LONG_ANALYSIS),
                Set.of("research.swarm", "research.web", "research.docs", "research.code", "research.paper", "memory.write")
        );
    }

    @Override
    public AgentStatus status() { return status; }

    @Override
    public CompletableFuture<AgentResponse> process(AgentRequest request) {
        status = AgentStatus.PROCESSING;
        totalRequests.incrementAndGet();
        long start = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            try {
                String query = request.payload();
                log.info("[ResearchSwarmAgent] starting swarm for: '{}'", query);

                List<SubQuery> subQueries = decomposeQuery(query);
                if (subQueries.isEmpty()) {
                    subQueries = List.of(new SubQuery(query, "web"));
                }
                log.info("[ResearchSwarmAgent] decomposed into {} sub-queries", subQueries.size());

                List<CompletableFuture<WorkerResult>> futures = subQueries.stream()
                        .limit(MAX_WORKERS)
                        .map(sq -> {
                            ResearchWorker worker = workerMap.get(sq.workerType());
                            if (worker == null) {
                                log.warn("[ResearchSwarmAgent] no worker for type '{}', using web", sq.workerType());
                                worker = workerMap.get("web");
                            }
                            if (worker == null) {
                                return CompletableFuture.completedFuture(
                                        WorkerResult.failed(sq.workerType(), sq.query(), "No worker available"));
                            }
                            return worker.execute(sq.query());
                        })
                        .toList();

                List<WorkerResult> results = futures.stream()
                        .map(CompletableFuture::join)
                        .toList();

                String consolidated = consolidateResults(query, results);
                var entry = memoryService.save(consolidated, MemoryType.PROJECT_KNOWLEDGE, "research-swarm");

                long elapsed = System.currentTimeMillis() - start;
                successful.incrementAndGet();
                totalLatencyMs += elapsed;
                status = AgentStatus.IDLE;

                long successCount = results.stream().filter(WorkerResult::success).count();
                eventPublisher.publishSwarmCompleted(SwarmCompletedEvent.of(
                        query, entry.getId(), elapsed, results.size(), successCount, UUID.randomUUID()));
                metrics.recordAgentInvocation("ResearchSwarmAgent", "SWARM_RESEARCH", true, elapsed);

                String summary = "Swarm research complete. %d workers (%d successful). Stored in Vault (id=%s):\n\n%s"
                        .formatted(results.size(), successCount, entry.getId(), consolidated);
                return AgentResponse.success(request, summary, 0.85, Duration.ofMillis(elapsed));
            } catch (Exception e) {
                failed.incrementAndGet();
                status = AgentStatus.ERROR;
                long elapsed = System.currentTimeMillis() - start;
                log.error("[ResearchSwarmAgent] failed: {}", e.getMessage(), e);
                return AgentResponse.failure(request, "Swarm research error: " + e.getMessage(),
                        Duration.ofMillis(elapsed));
            }
        });
    }

    List<SubQuery> decomposeQuery(String query) {
        String prompt = """
                Given this research query: "%s"
                Break it into up to %d sub-queries, each assigned to the most appropriate worker type.
                Worker types:
                - "web" for general web search
                - "docs" for technical documentation
                - "code" for code examples and repositories
                - "paper" for academic papers and articles

                Respond ONLY with a raw JSON array (no markdown, no extra text):
                [{"query": "...", "type": "web"}]
                """.formatted(query, MAX_WORKERS);

        try {
            LlmRequest llmRequest = LlmRequest.builder()
                    .messages(List.of(
                            Message.system("You are a research decomposition engine. Output only valid JSON."),
                            Message.user(prompt)
                    ))
                    .temperature(0.3)
                    .maxTokens(1024)
                    .build();

            LlmResponse response = providerRegistry.chat(llmRequest, TaskType.LONG_ANALYSIS).join();
            List<SubQuery> result = parseSubQueries(response.content());
            if (result.isEmpty()) {
                log.warn("[ResearchSwarmAgent] decomposed to empty list, falling back to single web query");
                return List.of(new SubQuery(query, "web"));
            }
            return result;
        } catch (Exception e) {
            log.warn("[ResearchSwarmAgent] decomposition failed: {}", e.getMessage());
            return List.of(new SubQuery(query, "web"));
        }
    }

    private List<SubQuery> parseSubQueries(String content) {
        String json = extractJsonArray(content);
        if (json == null) return List.of();
        try {
            List<Map<String, String>> items = objectMapper.readValue(json,
                    new TypeReference<List<Map<String, String>>>() {});
            return items.stream()
                    .filter(m -> m.containsKey("query") && m.containsKey("type"))
                    .map(m -> new SubQuery(m.get("query"), m.get("type")))
                    .toList();
        } catch (JsonProcessingException e) {
            log.warn("[ResearchSwarmAgent] failed to parse decomposition JSON: {}", e.getMessage());
            return List.of();
        }
    }

    private String extractJsonArray(String text) {
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }

    String consolidateResults(String query, List<WorkerResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("Research query: ").append(query).append("\n\n");

        for (WorkerResult r : results) {
            sb.append("--- ").append(r.workerType()).append(" worker ---\n");
            if (r.success()) {
                sb.append(r.content()).append("\n\n");
            } else {
                sb.append("Failed: ").append(r.error()).append("\n\n");
            }
        }

        String prompt = """
                Consolidate the following multi-source research results into a comprehensive summary.

                %s

                Provide a well-organized summary covering all key findings, organized by topic.
                Max 1000 words.
                """.formatted(sb);

        try {
            LlmRequest llmRequest = LlmRequest.builder()
                    .messages(List.of(
                            Message.system("You are a research consolidation engine. Synthesize findings from multiple sources."),
                            Message.user(prompt)
                    ))
                    .temperature(0.3)
                    .maxTokens(MAX_TOKENS_PER_SWARM)
                    .build();

            LlmResponse response = providerRegistry.chat(llmRequest, TaskType.LONG_ANALYSIS).join();
            return response.content();
        } catch (Exception e) {
            log.warn("[ResearchSwarmAgent] consolidation failed: {}", e.getMessage());
            return "Raw multi-source results:\n" + sb;
        }
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
