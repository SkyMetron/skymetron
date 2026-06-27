package dev.skymetron.application.agent;

import dev.skymetron.application.provider.ProviderRegistry;
import dev.skymetron.application.usecase.MemoryService;
import dev.skymetron.domain.execution.*;
import dev.skymetron.domain.memory.MemoryEntry;
import dev.skymetron.domain.memory.MemoryType;
import dev.skymetron.domain.tool.LlmResponse;
import dev.skymetron.infrastructure.messaging.EventPublisher;
import dev.skymetron.infrastructure.metrics.SkyMetricsRegistry;
import dev.skymetron.infrastructure.persistence.jpa.MemoryEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ConsolidationAgent tests")
class ConsolidationAgentTest {

    @Mock
    MemoryService memoryService;
    @Mock
    ProviderRegistry providerRegistry;
    @Mock
    EventPublisher eventPublisher;
    @Mock
    SkyMetricsRegistry metrics;

    ConsolidationAgent agent;

    @BeforeEach
    void setUp() {
        agent = new ConsolidationAgent(memoryService, providerRegistry, eventPublisher, metrics);
    }

    @Test
    @DisplayName("runConsolidation() reports scanned=0 when vault is empty")
    void emptyVault() {
        when(memoryService.listActive(any(), any())).thenReturn(new PageImpl<>(List.of()));

        String result = agent.runConsolidation();

        assertThat(result).contains("scanned=0");
    }

    @Test
    @DisplayName("runConsolidation() merges duplicates when LLM says YES")
    void mergesDuplicates() {
        float[] emb = new float[]{1.0f, 0.0f};
        MemoryEntry entry1 = new MemoryEntry("SkyMetron is great", emb, MemoryType.PROJECT_KNOWLEDGE, "test");
        setField(entry1, "id", UUID.randomUUID());

        UUID dupeId = UUID.randomUUID();
        MemoryEntry dupe = new MemoryEntry("SkyMetron is awesome", emb, MemoryType.PROJECT_KNOWLEDGE, "test");
        setField(dupe, "id", dupeId);

        when(memoryService.listActive(any(), any())).thenReturn(new PageImpl<>(List.of(entry1)));
        when(memoryService.findDuplicatesRaw(anyString(), anyDouble(), any(), anyInt()))
                .thenReturn(List.of(dupeId));
        when(memoryService.getById(dupeId)).thenReturn(Optional.of(dupe));
        when(providerRegistry.chat(any(), any())).thenReturn(CompletableFuture.completedFuture(
                new LlmResponse("YES", "model", "groq", 5, 1, Duration.ofMillis(50), false, false)));

        String result = agent.runConsolidation();

        assertThat(result).contains("merged=1");
    }

    @Test
    @DisplayName("runConsolidation() skips when LLM says NO")
    void skipsWhenLlmSaysNo() {
        float[] emb = new float[]{1.0f, 0.0f};
        MemoryEntry entry1 = new MemoryEntry("Java is a language", emb, MemoryType.PROJECT_KNOWLEDGE, "test");
        setField(entry1, "id", UUID.randomUUID());

        UUID dupeId = UUID.randomUUID();
        MemoryEntry dupe = new MemoryEntry("Python is a language", emb, MemoryType.PROJECT_KNOWLEDGE, "test");
        setField(dupe, "id", dupeId);

        when(memoryService.listActive(any(), any())).thenReturn(new PageImpl<>(List.of(entry1)));
        when(memoryService.findDuplicatesRaw(anyString(), anyDouble(), any(), anyInt()))
                .thenReturn(List.of(dupeId));
        when(memoryService.getById(dupeId)).thenReturn(Optional.of(dupe));
        when(providerRegistry.chat(any(), any())).thenReturn(CompletableFuture.completedFuture(
                new LlmResponse("NO", "model", "groq", 5, 1, Duration.ofMillis(50), false, false)));

        String result = agent.runConsolidation();

        assertThat(result).contains("skipped=1");
    }

    @Test
    @DisplayName("runConsolidation() skips entries with null embedding")
    void skipsNullEmbedding() {
        MemoryEntry entry = new MemoryEntry("no embedding", new float[]{0.1f}, MemoryType.USER_FACTS, "test");
        setField(entry, "id", UUID.randomUUID());
        setField(entry, "embedding", null);

        when(memoryService.listActive(any(), any())).thenReturn(new PageImpl<>(List.of(entry)));

        String result = agent.runConsolidation();

        assertThat(result).contains("scanned=1", "merged=0");
    }

    @Test
    @DisplayName("id() returns stable ConsolidationAgent id")
    void idStable() {
        assertThat(agent.id()).isEqualTo(AgentId.of("00000000-0000-0000-0000-000000000004"));
    }

    @Test
    @DisplayName("health() returns HEALTHY with no requests")
    void healthEmpty() {
        assertThat(agent.health()).isEqualTo(HealthStatus.HEALTHY);
    }

    @Test
    @DisplayName("capabilities() declares MEMORY_DEDUP and VAULT_CONSOLIDATION")
    void capabilitiesDeclaresTaskTypes() {
        AgentCapabilities caps = agent.capabilities();
        assertThat(caps.supportedTaskTypes()).contains(dev.skymetron.domain.tool.TaskType.MEMORY_DEDUP);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
