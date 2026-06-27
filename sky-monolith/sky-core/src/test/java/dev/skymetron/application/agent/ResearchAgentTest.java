package dev.skymetron.application.agent;

import dev.skymetron.application.provider.ProviderRegistry;
import dev.skymetron.application.usecase.MemoryService;
import dev.skymetron.domain.execution.*;
import dev.skymetron.domain.memory.MemoryEntry;
import dev.skymetron.domain.memory.MemoryType;
import dev.skymetron.domain.tool.LlmResponse;
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
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ResearchAgent tests")
class ResearchAgentTest {

    @Mock
    ProviderRegistry providerRegistry;
    @Mock
    MemoryService memoryService;
    @Mock
    WebSearchWorker webWorker;
    @Mock
    EventPublisher eventPublisher;
    @Mock
    SkyMetricsRegistry metrics;

    ResearchAgent researchAgent;

    @BeforeEach
    void setUp() {
        researchAgent = new ResearchAgent(providerRegistry, memoryService, webWorker, eventPublisher, metrics);
    }

    @Test
    @DisplayName("process() returns summarized research and stores in Vault")
    void processResearchSuccess() {
        when(webWorker.search("Spring Boot 3")).thenReturn("1. Spring Boot docs\n2. Spring guides");
        when(providerRegistry.chat(any(), any())).thenReturn(CompletableFuture.completedFuture(
                new LlmResponse("Spring Boot 3 is a Java framework.", "model", "groq", 50, 20, Duration.ofMillis(200), false, false)));

        float[] emb = new float[]{0.1f};
        MemoryEntry entry = new MemoryEntry("summary", emb, MemoryType.PROJECT_KNOWLEDGE, "research");
        when(memoryService.save(anyString(), eq(MemoryType.PROJECT_KNOWLEDGE), eq("research"))).thenReturn(entry);

        AgentRequest request = AgentRequest.simple(researchAgent.id(), Intent.RESEARCH, "Spring Boot 3");
        AgentResponse response = researchAgent.process(request).join();

        assertThat(response.success()).isTrue();
        assertThat(response.content()).contains("Research complete");
    }

    @Test
    @DisplayName("process() handles web search failure gracefully")
    void processSearchFailure() {
        when(webWorker.search(anyString())).thenReturn("Search failed: timeout");
        when(providerRegistry.chat(any(), any())).thenReturn(CompletableFuture.completedFuture(
                new LlmResponse("No results found.", "model", "groq", 10, 5, Duration.ofMillis(100), false, false)));

        float[] emb = new float[]{0.1f};
        MemoryEntry entry = new MemoryEntry("no results", emb, MemoryType.PROJECT_KNOWLEDGE, "research");
        when(memoryService.save(anyString(), any(), anyString())).thenReturn(entry);

        AgentRequest request = AgentRequest.simple(researchAgent.id(), Intent.RESEARCH, "nonexistent topic");
        AgentResponse response = researchAgent.process(request).join();

        assertThat(response.success()).isTrue();
    }

    @Test
    @DisplayName("process() handles LLM failure by returning raw results")
    void processLlmFailure() {
        when(webWorker.search("test")).thenReturn("Some raw results here");
        when(providerRegistry.chat(any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("LLM down")));

        float[] emb = new float[]{0.1f};
        MemoryEntry entry = new MemoryEntry("raw", emb, MemoryType.PROJECT_KNOWLEDGE, "research");
        when(memoryService.save(anyString(), any(), anyString())).thenReturn(entry);

        AgentRequest request = AgentRequest.simple(researchAgent.id(), Intent.RESEARCH, "test");
        AgentResponse response = researchAgent.process(request).join();

        assertThat(response.success()).isTrue();
        assertThat(response.content()).contains("Raw results");
    }

    @Test
    @DisplayName("id() returns stable ResearchAgent id")
    void idStable() {
        assertThat(researchAgent.id()).isEqualTo(AgentId.of("00000000-0000-0000-0000-000000000005"));
    }

    @Test
    @DisplayName("capabilities() declares RESEARCH intent")
    void capabilitiesDeclaresIntent() {
        AgentCapabilities caps = researchAgent.capabilities();
        assertThat(caps.supports(Intent.RESEARCH)).isTrue();
    }

    @Test
    @DisplayName("health() returns HEALTHY with no requests")
    void healthEmpty() {
        assertThat(researchAgent.health()).isEqualTo(HealthStatus.HEALTHY);
    }
}
