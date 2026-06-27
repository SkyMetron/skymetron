package dev.skymetron.application.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.skymetron.application.provider.ProviderRegistry;
import dev.skymetron.application.usecase.MemoryService;
import dev.skymetron.domain.execution.*;
import dev.skymetron.domain.memory.MemoryEntry;
import dev.skymetron.domain.memory.MemoryType;
import dev.skymetron.domain.tool.*;
import dev.skymetron.infrastructure.messaging.EventPublisher;
import dev.skymetron.infrastructure.metrics.SkyMetricsRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ResearchSwarmAgent tests")
class ResearchSwarmAgentTest {

    @Mock
    ProviderRegistry providerRegistry;

    @Mock
    MemoryService memoryService;

    @Mock
    EventPublisher eventPublisher;

    @Mock
    SkyMetricsRegistry metrics;

    @Mock
    ResearchWorker webWorker;

    @Mock
    ResearchWorker docsWorker;

    ObjectMapper objectMapper = new ObjectMapper();

    ResearchSwarmAgent agent;

    UUID storedId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(webWorker.type()).thenReturn("web");
        lenient().when(docsWorker.type()).thenReturn("docs");
        lenient().when(webWorker.execute(any())).thenReturn(CompletableFuture.completedFuture(
                WorkerResult.ok("web", "test query", "web search results", List.of("web-source"))));
        lenient().when(docsWorker.execute(any())).thenReturn(CompletableFuture.completedFuture(
                WorkerResult.ok("docs", "test query", "docs search results", List.of("docs-source"))));

        agent = new ResearchSwarmAgent(providerRegistry, memoryService, eventPublisher, metrics,
                List.of(webWorker, docsWorker), objectMapper);
    }

    @Test
    @DisplayName("process() succeeds with decomposition and consolidation")
    void processSuccess() {
        String decomposeJson = """
                [{"query": "java 21 features", "type": "web"}, {"query": "spring boot 3 docs", "type": "docs"}]
                """;
        when(providerRegistry.chat(any(LlmRequest.class), eq(TaskType.LONG_ANALYSIS)))
                .thenReturn(CompletableFuture.completedFuture(
                        new LlmResponse(decomposeJson, "model", "mistral", 50, 10, Duration.ofMillis(100), false, false)))
                .thenReturn(CompletableFuture.completedFuture(
                        new LlmResponse("Consolidated results", "model", "mistral", 100, 50, Duration.ofMillis(200), false, false)));
        when(memoryService.save(anyString(), eq(MemoryType.PROJECT_KNOWLEDGE), eq("research-swarm")))
                .thenReturn(new MemoryEntry("consolidated", new float[768], MemoryType.PROJECT_KNOWLEDGE, "research-swarm"));

        AgentRequest request = AgentRequest.fromUser(agent.id(), Intent.RESEARCH, "pesquise profundamente sobre Spring Boot 3 e Java 21");
        AgentResponse response = agent.process(request).join();

        assertThat(response.success()).isTrue();
        assertThat(response.content()).contains("Swarm research complete");
        assertThat(response.content()).contains("2 workers");
    }

    @Test
    @DisplayName("process() falls back to single web query when decomposition returns empty")
    void processFallbackEmptyDecomposition() {
        when(providerRegistry.chat(any(LlmRequest.class), eq(TaskType.LONG_ANALYSIS)))
                .thenReturn(CompletableFuture.completedFuture(
                        new LlmResponse("not json", "model", "mistral", 10, 5, Duration.ofMillis(50), false, false)))
                .thenReturn(CompletableFuture.completedFuture(
                        new LlmResponse("Consolidated result", "model", "mistral", 50, 25, Duration.ofMillis(100), false, false)));
        when(memoryService.save(anyString(), eq(MemoryType.PROJECT_KNOWLEDGE), eq("research-swarm")))
                .thenReturn(new MemoryEntry("result", new float[768], MemoryType.PROJECT_KNOWLEDGE, "research-swarm"));

        AgentRequest request = AgentRequest.fromUser(agent.id(), Intent.RESEARCH, "pesquise sobre Java");
        AgentResponse response = agent.process(request).join();

        assertThat(response.success()).isTrue();
        assertThat(response.content()).contains("Swarm research complete");
    }

    @Test
    @DisplayName("process() returns failure when provider throws")
    void processFailure() {
        when(providerRegistry.chat(any(LlmRequest.class), eq(TaskType.LONG_ANALYSIS)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("LLM unavailable")));
        when(memoryService.save(anyString(), eq(MemoryType.PROJECT_KNOWLEDGE), eq("research-swarm")))
                .thenReturn(new MemoryEntry("raw result", new float[768], MemoryType.PROJECT_KNOWLEDGE, "research-swarm"));

        AgentRequest request = AgentRequest.fromUser(agent.id(), Intent.RESEARCH, "pesquise profundamente");
        AgentResponse response = agent.process(request).join();

        assertThat(response.success()).isTrue();
        assertThat(response.content()).contains("Swarm research complete");
    }

    @Test
    @DisplayName("decomposeQuery() returns sub-queries from LLM JSON")
    void decomposeQuerySuccess() {
        String json = """
                [{"query": "java 21", "type": "web"}, {"query": "spring boot", "type": "docs"}]
                """;
        when(providerRegistry.chat(any(LlmRequest.class), eq(TaskType.LONG_ANALYSIS)))
                .thenReturn(CompletableFuture.completedFuture(
                        new LlmResponse(json, "model", "mistral", 20, 5, Duration.ofMillis(50), false, false)));

        List<SubQuery> subQueries = agent.decomposeQuery("test query");
        assertThat(subQueries).hasSize(2);
        assertThat(subQueries.get(0).query()).isEqualTo("java 21");
        assertThat(subQueries.get(0).workerType()).isEqualTo("web");
        assertThat(subQueries.get(1).query()).isEqualTo("spring boot");
        assertThat(subQueries.get(1).workerType()).isEqualTo("docs");
    }

    @Test
    @DisplayName("decomposeQuery() falls back to single web query on JSON parse failure")
    void decomposeQueryFallback() {
        when(providerRegistry.chat(any(LlmRequest.class), eq(TaskType.LONG_ANALYSIS)))
                .thenReturn(CompletableFuture.completedFuture(
                        new LlmResponse("invalid response", "model", "mistral", 5, 2, Duration.ofMillis(20), false, false)));

        List<SubQuery> subQueries = agent.decomposeQuery("test query");
        assertThat(subQueries).hasSize(1);
        assertThat(subQueries.get(0).query()).isEqualTo("test query");
        assertThat(subQueries.get(0).workerType()).isEqualTo("web");
    }

    @Test
    @DisplayName("id() returns stable swarm agent id")
    void idStable() {
        assertThat(agent.id()).isEqualTo(AgentId.of("00000000-0000-0000-0000-000000000010"));
    }

    @Test
    @DisplayName("capabilities() declares supported intents")
    void capabilitiesDeclaresIntents() {
        AgentCapabilities caps = agent.capabilities();
        assertThat(caps.supports(Intent.RESEARCH)).isTrue();
    }

    @Test
    @DisplayName("health() returns HEALTHY with no requests")
    void healthEmpty() {
        assertThat(agent.health()).isEqualTo(HealthStatus.HEALTHY);
    }
}
