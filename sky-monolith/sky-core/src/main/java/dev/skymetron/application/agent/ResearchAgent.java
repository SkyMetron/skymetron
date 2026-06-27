package dev.skymetron.application.agent;

import dev.skymetron.application.provider.ProviderRegistry;
import dev.skymetron.application.usecase.MemoryService;
import dev.skymetron.domain.execution.*;
import dev.skymetron.domain.memory.MemoryType;
import dev.skymetron.domain.observation.ResearchCompletedEvent;
import dev.skymetron.domain.tool.*;
import dev.skymetron.infrastructure.messaging.EventPublisher;
import dev.skymetron.infrastructure.metrics.SkyMetricsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Research Agent — searches the web, spawns temporary workers, consolidates
 * results, and stores findings in the Vault.
 *
 * <p>Uses {@link WebSearchTool} for actual web queries. Results are summarized
 * by an LLM and saved to the Vault as PROJECT_KNOWLEDGE.
 *
 * <p>Workers are differentiated by source type but in Sprint 5 only
 * {@code WebResearchWorker} is implemented. Sprint 10 adds Docs/Code/Paper workers.
 */
@Component
public class ResearchAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(ResearchAgent.class);
    private static final AgentId RESEARCH_ID = AgentId.of("00000000-0000-0000-0000-000000000005");

    private final ProviderRegistry providerRegistry;
    private final MemoryService memoryService;
    private final WebSearchWorker webWorker;
    private final EventPublisher eventPublisher;
    private final SkyMetricsRegistry metrics;

    private volatile AgentStatus status = AgentStatus.IDLE;
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successful = new AtomicLong(0);
    private final AtomicLong failed = new AtomicLong(0);
    private double totalLatencyMs = 0;

    public ResearchAgent(ProviderRegistry providerRegistry, MemoryService memoryService,
                         WebSearchWorker webWorker, EventPublisher eventPublisher, SkyMetricsRegistry metrics) {
        this.providerRegistry = providerRegistry;
        this.memoryService = memoryService;
        this.webWorker = webWorker;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
    }

    @Override
    public AgentId id() { return RESEARCH_ID; }

    @Override
    public AgentCapabilities capabilities() {
        return new AgentCapabilities(
                Set.of(Intent.RESEARCH),
                Set.of(TaskType.WEB_RESEARCH),
                Set.of("research.web", "memory.write")
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
                String query = request.payload();
                log.info("[ResearchAgent] starting research: '{}'", query);

                String rawResults = webWorker.search(query);
                String summary = summarizeWithLlm(query, rawResults);

                var entry = memoryService.save(summary, MemoryType.PROJECT_KNOWLEDGE, "research");
                log.info("[ResearchAgent] stored research result in Vault id={}", entry.getId());

                long elapsed = java.lang.System.currentTimeMillis() - start;
                successful.incrementAndGet();
                totalLatencyMs += elapsed;
                status = AgentStatus.IDLE;

                eventPublisher.publishResearchCompleted(ResearchCompletedEvent.of(
                        query, entry.getId(), elapsed, 1, java.util.UUID.randomUUID()));
                metrics.recordAgentInvocation("ResearchAgent", "RESEARCH", true, elapsed);

                String response = "Research complete. Summary stored in Vault (id=" + entry.getId() + "):\n\n" + summary;
                return AgentResponse.success(request, response, 0.8, Duration.ofMillis(elapsed));
            } catch (Exception e) {
                failed.incrementAndGet();
                status = AgentStatus.ERROR;
                long elapsed = java.lang.System.currentTimeMillis() - start;
                log.error("[ResearchAgent] failed: {}", e.getMessage(), e);
                return AgentResponse.failure(request, "Research error: " + e.getMessage(),
                        Duration.ofMillis(elapsed));
            }
        });
    }

    /**
     * Summarize raw search results using an LLM.
     */
    private String summarizeWithLlm(String query, String rawResults) {
        String prompt = String.format("""
                Research query: %s

                Raw web search results:
                %s

                Provide a concise summary of the key findings (max 500 words).
                """, query, truncate(rawResults, 3000));

        try {
            LlmRequest request = LlmRequest.builder()
                    .messages(List.of(
                            Message.system("You are a research assistant. Summarize findings concisely."),
                            Message.user(prompt)
                    ))
                    .temperature(0.3)
                    .maxTokens(1024)
                    .build();

            LlmResponse response = providerRegistry.chat(request, TaskType.WEB_RESEARCH).join();
            return response.content();
        } catch (Exception e) {
            log.warn("[ResearchAgent] LLM summarization failed, returning raw results: {}", e.getMessage());
            return "Raw results:\n" + truncate(rawResults, 1000);
        }
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
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
