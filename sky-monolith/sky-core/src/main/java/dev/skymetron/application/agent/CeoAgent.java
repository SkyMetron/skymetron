package dev.skymetron.application.agent;

import dev.skymetron.application.provider.ProviderRegistry;
import dev.skymetron.application.usecase.SessionBudget;
import dev.skymetron.domain.execution.*;
import dev.skymetron.domain.observation.AgentInvokedEvent;
import dev.skymetron.domain.tool.*;
import dev.skymetron.infrastructure.messaging.EventPublisher;
import dev.skymetron.infrastructure.metrics.SkyMetricsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The CEO Agent — the first agent users interact with.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Classify the user's intent (memory, tool, research, general)</li>
 *   <li>Select the best LLM provider for the task type</li>
 *   <li>Route to a downstream agent or respond directly via the ProviderRegistry</li>
 *   <li>Handle fallback gracefully (never crash on provider failure)</li>
 * </ol>
 *
 * <p>Brain Trace: every decision (which provider, why, fallback) is logged.
 */
@Component
public class CeoAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(CeoAgent.class);
    private static final AgentId CEO_ID = AgentId.of("00000000-0000-0000-0000-000000000001");

    private final ProviderRegistry providerRegistry;
    private final SessionBudget budget;
    private final MemoryAgent memoryAgent;
    private final ToolAgent toolAgent;
    private final ResearchAgent researchAgent;
    private final ResearchSwarmAgent researchSwarmAgent;
    private final KnowledgeAgent knowledgeAgent;
    private final QaAgent qaAgent;
    private final SecurityAgent securityAgent;
    private final EventPublisher eventPublisher;
    private final SkyMetricsRegistry metrics;

    private volatile AgentStatus status = AgentStatus.IDLE;
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successful = new AtomicLong(0);
    private final AtomicLong failed = new AtomicLong(0);
    private double totalLatencyMs = 0;

    public CeoAgent(ProviderRegistry providerRegistry, SessionBudget budget,
                    MemoryAgent memoryAgent, ToolAgent toolAgent, ResearchAgent researchAgent,
                    ResearchSwarmAgent researchSwarmAgent, KnowledgeAgent knowledgeAgent,
                    QaAgent qaAgent, SecurityAgent securityAgent,
                    EventPublisher eventPublisher, SkyMetricsRegistry metrics) {
        this.providerRegistry = providerRegistry;
        this.budget = budget;
        this.memoryAgent = memoryAgent;
        this.toolAgent = toolAgent;
        this.researchAgent = researchAgent;
        this.researchSwarmAgent = researchSwarmAgent;
        this.knowledgeAgent = knowledgeAgent;
        this.qaAgent = qaAgent;
        this.securityAgent = securityAgent;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
    }

    @Override
    public AgentId id() { return CEO_ID; }

    @Override
    public AgentCapabilities capabilities() {
        return new AgentCapabilities(
                Set.of(Intent.GENERAL_CHAT, Intent.CODE_HELP, Intent.SYSTEM_CONTROL, Intent.MEMORY_QUERY,
                        Intent.SKILL_MANAGEMENT, Intent.QA_TEST, Intent.SECURITY_ANALYSIS, Intent.RESEARCH_SWARM),
                Set.of(TaskType.CODE_GENERATION, TaskType.LONG_ANALYSIS, TaskType.FAST_RESPONSE,
                        TaskType.DOCUMENTATION, TaskType.QA_TESTING, TaskType.SECURITY, TaskType.GENERAL),
                Set.of("llm.route"));
    }

    @Override
    public AgentStatus status() { return status; }

    @Override
    public CompletableFuture<AgentResponse> process(AgentRequest request) {
        status = AgentStatus.PROCESSING;
        totalRequests.incrementAndGet();
        Instant start = Instant.now();
        long traceStart = java.lang.System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            try {
                Intent intent = classifyIntent(request.payload());
                log.info("[CEO] intent={} for request {}", intent, request.id());

                AgentResponse response;
                if (intent == Intent.MEMORY_QUERY || intent == Intent.MEMORY_STORE) {
                    log.info("[CEO] routing to MemoryAgent");
                    response = memoryAgent.process(request).join();
                } else if (intent == Intent.TOOL_EXECUTION) {
                    log.info("[CEO] routing to ToolAgent");
                    response = toolAgent.process(request).join();
                } else if (intent == Intent.RESEARCH) {
                    log.info("[CEO] routing to ResearchAgent");
                    response = researchAgent.process(request).join();
                } else if (intent == Intent.RESEARCH_SWARM) {
                    log.info("[CEO] routing to ResearchSwarmAgent");
                    response = researchSwarmAgent.process(request).join();
                } else if (intent == Intent.SKILL_MANAGEMENT) {
                    log.info("[CEO] routing to KnowledgeAgent");
                    response = knowledgeAgent.process(request).join();
                } else if (intent == Intent.QA_TEST) {
                    log.info("[CEO] routing to QaAgent");
                    response = qaAgent.process(request).join();
                } else if (intent == Intent.SECURITY_ANALYSIS) {
                    log.info("[CEO] routing to SecurityAgent");
                    response = securityAgent.process(request).join();
                } else {
                    TaskType taskType = selectTaskType(intent, request.payload());
                    log.info("[CEO] intent={} taskType={} routing to LLM", intent, taskType);

                    LlmRequest llmRequest = buildLlmRequest(request, taskType);
                    LlmResponse llmResponse = providerRegistry.chat(llmRequest, taskType).join();

                    budget.recordUsage(llmResponse.promptTokens() + llmResponse.completionTokens());
                    response = AgentResponse.success(request, llmResponse.content(), 0.9,
                            Duration.ofMillis(java.lang.System.currentTimeMillis() - traceStart));
                }

                long elapsed = java.lang.System.currentTimeMillis() - traceStart;
                successful.incrementAndGet();
                totalLatencyMs += elapsed;
                status = AgentStatus.IDLE;
                log.info("[CEO] completed in {}ms", elapsed);

                eventPublisher.publishAgentInvoked(AgentInvokedEvent.of(
                        id().toString(), "CeoAgent", intent.name(),
                        request.id().value().toString(), elapsed, response.success(),
                        request.context().traceId()));
                metrics.recordAgentInvocation("CeoAgent", intent.name(), response.success(), elapsed);
                return response;
            } catch (Exception e) {
                failed.incrementAndGet();
                status = AgentStatus.ERROR;
                long elapsed = java.lang.System.currentTimeMillis() - traceStart;
                log.error("[CEO] failed to process request {}: {}", request.id(), e.getMessage(), e);
                return AgentResponse.failure(request, "CEO error: " + e.getMessage(),
                        Duration.ofMillis(elapsed));
            }
        });
    }

    /**
     * Classify user intent from the message text.
     * Simple keyword-based for Sprint 3; will use LLM classification in Sprint 4+.
     */
    public Intent classifyIntent(String text) {
        if (text == null || text.isBlank()) return Intent.GENERAL_CHAT;
        String lower = text.toLowerCase();
        if (lower.contains("lembre") || lower.contains("salve isso")
                || lower.contains("guarde") || lower.startsWith("memorize")) {
            return Intent.MEMORY_STORE;
        }
        if (lower.contains("o que voce sabe") || lower.contains("o que você sabe")
                || lower.contains("lembra de") || lower.contains("vault")
                || lower.contains("memoria sobre") || lower.contains("memória sobre")) {
            return Intent.MEMORY_QUERY;
        }
        if (lower.startsWith("filesystem") || lower.startsWith("arquivo")
                || lower.startsWith("liste arquivos") || lower.startsWith("listar")
                || lower.contains("list files") || lower.startsWith("execute tool")) {
            return Intent.TOOL_EXECUTION;
        }
        if (lower.contains("codigo") || lower.contains("funcao") || lower.contains("classe")
                || lower.contains("bug") || lower.contains("refator")) {
            return Intent.CODE_HELP;
        }
        if (lower.contains("pesquise") || lower.contains("pesquisa") || lower.contains("busque")) {
            if (lower.contains("completa") || lower.contains("profunda") || lower.contains("multi")
                    || lower.contains("tudo sobre") || lower.contains("detalhada")
                    || lower.contains("swarm") || lower.contains("exaustiva")
                    || lower.contains("complete research") || lower.contains("deep research")) {
                return Intent.RESEARCH_SWARM;
            }
            return Intent.RESEARCH;
        }
        if (lower.startsWith("skill") || lower.startsWith("add|") && lower.contains("descri")
                || lower.startsWith("knowledge")) {
            return Intent.SKILL_MANAGEMENT;
        }
        if (lower.startsWith("qa|") || lower.startsWith("test|") || lower.startsWith("run|")) {
            return Intent.QA_TEST;
        }
        if (lower.startsWith("scan|") || lower.startsWith("security") || lower.contains("vulnerabilidade")
                || lower.contains("seguranca") || lower.contains("segurança")) {
            return Intent.SECURITY_ANALYSIS;
        }
        return Intent.GENERAL_CHAT;
    }

    /**
     * Select the TaskType for provider routing based on intent + content.
     */
    public TaskType selectTaskType(Intent intent, String text) {
        return switch (intent) {
            case MEMORY_QUERY -> TaskType.FAST_RESPONSE;
            case CODE_HELP -> TaskType.CODE_GENERATION;
            case RESEARCH -> TaskType.WEB_RESEARCH;
            case RESEARCH_SWARM -> TaskType.LONG_ANALYSIS;
            case TOOL_EXECUTION -> TaskType.FAST_RESPONSE;
            case MEMORY_STORE -> TaskType.FAST_RESPONSE;
            case SYSTEM_CONTROL -> TaskType.FAST_RESPONSE;
            case SKILL_MANAGEMENT -> TaskType.FAST_RESPONSE;
            case QA_TEST -> TaskType.QA_TESTING;
            case SECURITY_ANALYSIS -> TaskType.SECURITY;
            case GENERAL_CHAT -> {
                if (text != null && text.length() > 2000) yield TaskType.LONG_ANALYSIS;
                yield TaskType.FAST_RESPONSE;
            }
        };
    }

    private LlmRequest buildLlmRequest(AgentRequest request, TaskType taskType) {
        return LlmRequest.builder()
                .messages(List.of(
                        Message.system("You are SkyMetron, an AI operating system. Respond concisely in the user's language."),
                        Message.user(request.payload())
                ))
                .temperature(taskType == TaskType.CODE_GENERATION ? 0.2 : 0.7)
                .maxTokens(Math.min(budget.maxTokensPerRequest(), 2048))
                .metadata(java.util.Map.of("taskType", taskType.name(), "intent",
                        classifyIntent(request.payload()).name()))
                .build();
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
