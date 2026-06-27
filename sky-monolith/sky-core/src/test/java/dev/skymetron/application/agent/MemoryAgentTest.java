package dev.skymetron.application.agent;

import dev.skymetron.application.usecase.MemoryService;
import dev.skymetron.domain.execution.*;
import dev.skymetron.domain.memory.MemoryEntry;
import dev.skymetron.domain.memory.MemoryType;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MemoryAgent tests")
class MemoryAgentTest {

    @Mock
    MemoryService memoryService;
    @Mock
    EventPublisher eventPublisher;
    @Mock
    SkyMetricsRegistry metrics;

    MemoryAgent memoryAgent;

    @BeforeEach
    void setUp() {
        memoryAgent = new MemoryAgent(memoryService, eventPublisher, metrics);
    }

    @Test
    @DisplayName("MEMORY_QUERY returns formatted search results")
    void queryReturnsResults() {
        var hit = new MemoryService.SearchHit(UUID.randomUUID(), "SkyMetron uses Java 21", "test", 0.9, 0.95);
        when(memoryService.search("java", null, 5)).thenReturn(List.of(hit));

        AgentRequest req = AgentRequest.simple(memoryAgent.id(), Intent.MEMORY_QUERY, "java");
        AgentResponse resp = memoryAgent.process(req).join();

        assertThat(resp.success()).isTrue();
        assertThat(resp.content()).contains("Found 1 memories");
        assertThat(resp.content()).contains("SkyMetron uses Java 21");
    }

    @Test
    @DisplayName("MEMORY_QUERY with no results returns message")
    void queryNoResults() {
        when(memoryService.search("nonexistent", null, 5)).thenReturn(List.of());

        AgentRequest req = AgentRequest.simple(memoryAgent.id(), Intent.MEMORY_QUERY, "nonexistent");
        AgentResponse resp = memoryAgent.process(req).join();

        assertThat(resp.success()).isTrue();
        assertThat(resp.content()).contains("No memories found");
    }

    @Test
    @DisplayName("MEMORY_STORE saves fact to Vault")
    void storeSavesFact() {
        float[] emb = new float[]{0.1f};
        MemoryEntry entry = new MemoryEntry("test fact", emb, MemoryType.USER_FACTS, "user");
        when(memoryService.save(anyString(), eq(MemoryType.USER_FACTS), eq("user"))).thenReturn(entry);

        AgentRequest req = AgentRequest.simple(memoryAgent.id(), Intent.MEMORY_STORE, "remember this");
        AgentResponse resp = memoryAgent.process(req).join();

        assertThat(resp.success()).isTrue();
        assertThat(resp.content()).contains("Memory stored");
    }

    @Test
    @DisplayName("MEMORY_STORE with type prefix parses type correctly")
    void storeWithTypePrefix() {
        float[] emb = new float[]{0.1f};
        MemoryEntry entry = new MemoryEntry("proj info", emb, MemoryType.PROJECT_KNOWLEDGE, "user");
        when(memoryService.save(anyString(), eq(MemoryType.PROJECT_KNOWLEDGE), eq("user"))).thenReturn(entry);

        AgentRequest req = AgentRequest.simple(memoryAgent.id(), Intent.MEMORY_STORE,
                "PROJECT_KNOWLEDGE|The architecture uses pgvector");
        AgentResponse resp = memoryAgent.process(req).join();

        assertThat(resp.success()).isTrue();
    }

    @Test
    @DisplayName("process() handles exceptions gracefully")
    void processHandlesException() {
        when(memoryService.search(anyString(), any(), anyInt()))
                .thenThrow(new RuntimeException("Vault offline"));

        AgentRequest req = AgentRequest.simple(memoryAgent.id(), Intent.MEMORY_QUERY, "test");
        AgentResponse resp = memoryAgent.process(req).join();

        assertThat(resp.success()).isFalse();
        assertThat(resp.content()).contains("Vault offline");
    }

    @Test
    @DisplayName("id() returns stable MemoryAgent id")
    void idStable() {
        assertThat(memoryAgent.id()).isEqualTo(AgentId.of("00000000-0000-0000-0000-000000000002"));
    }

    @Test
    @DisplayName("capabilities() declares memory intents")
    void capabilitiesDeclaresIntents() {
        AgentCapabilities caps = memoryAgent.capabilities();
        assertThat(caps.supports(Intent.MEMORY_QUERY)).isTrue();
        assertThat(caps.supports(Intent.MEMORY_STORE)).isTrue();
        assertThat(caps.supports(Intent.GENERAL_CHAT)).isFalse();
    }

    @Test
    @DisplayName("health() returns HEALTHY with no requests")
    void healthEmpty() {
        assertThat(memoryAgent.health()).isEqualTo(HealthStatus.HEALTHY);
    }
}
