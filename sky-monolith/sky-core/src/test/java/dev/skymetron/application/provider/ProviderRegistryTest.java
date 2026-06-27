package dev.skymetron.application.provider;

import dev.skymetron.domain.tool.*;
import dev.skymetron.infrastructure.messaging.EventPublisher;
import dev.skymetron.infrastructure.metrics.SkyMetricsRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("ProviderRegistry tests")
class ProviderRegistryTest {

    private TestProvider mistral;
    private TestProvider nvidia;
    private TestProvider ollama;
    private ProviderRegistry registry;

    @BeforeEach
    void setUp() {
        mistral = new TestProvider("mistral", true);
        nvidia = new TestProvider("nvidia", true);
        ollama = new TestProvider("ollama-local", true);
        registry = new ProviderRegistry(List.of(mistral, nvidia, ollama), mock(EventPublisher.class), mock(SkyMetricsRegistry.class));
    }

    @Test
    @DisplayName("routingOrder returns correct preference for CODE_GENERATION")
    void routingOrderCodeGeneration() {
        List<String> order = registry.routingOrder(TaskType.CODE_GENERATION);
        assertThat(order).startsWith("mistral", "nvidia");
    }

    @Test
    @DisplayName("routingOrder returns ollama-only for OFFLINE")
    void routingOrderOffline() {
        List<String> order = registry.routingOrder(TaskType.OFFLINE);
        assertThat(order).containsExactly("ollama-local");
    }

    @Test
    @DisplayName("chat() uses first available provider in routing order")
    void chatUsesFirstAvailable() {
        LlmRequest req = LlmRequest.builder().messages(List.of(Message.user("hi"))).build();
        LlmResponse resp = registry.chat(req, TaskType.GENERAL).join();

        assertThat(resp.content()).isEqualTo("mistral-response");
        assertThat(resp.providerId()).isEqualTo("mistral");
        assertThat(resp.fromFallback()).isFalse();
    }

    @Test
    @DisplayName("chat() falls back to next provider when first fails")
    void chatFallsBackOnError() {
        mistral.failNext = true;
        LlmRequest req = LlmRequest.builder().messages(List.of(Message.user("hi"))).build();
        LlmResponse resp = registry.chat(req, TaskType.CODE_GENERATION).join();

        assertThat(resp.providerId()).isEqualTo("nvidia");
        assertThat(resp.fromFallback()).isTrue();
    }

    @Test
    @DisplayName("chat() throws ProviderUnavailableException when all fail")
    void chatAllFail() {
        mistral.failNext = true;
        nvidia.failNext = true;
        ollama.failNext = true;
        LlmRequest req = LlmRequest.builder().messages(List.of(Message.user("hi"))).build();

        assertThatThrownBy(() -> registry.chat(req, TaskType.GENERAL).join())
                .hasCauseInstanceOf(ProviderRegistry.ProviderUnavailableException.class);
    }

    @Test
    @DisplayName("chat() skips unavailable providers")
    void chatSkipsUnavailable() {
        mistral.available = false;
        LlmRequest req = LlmRequest.builder().messages(List.of(Message.user("hi"))).build();
        LlmResponse resp = registry.chat(req, TaskType.CODE_GENERATION).join();

        assertThat(resp.providerId()).isEqualTo("nvidia");
    }

    @Test
    @DisplayName("getStatus() returns all providers in fallback chain")
    void getStatus() {
        List<ProviderRegistry.ProviderStatus> status = registry.getStatus();
        assertThat(status).hasSizeGreaterThanOrEqualTo(6);
        assertThat(status.get(0).providerId()).isEqualTo("mistral");
    }

    static class TestProvider implements LlmProvider {
        private final String id;
        private boolean available;
        private boolean failNext = false;

        TestProvider(String id, boolean available) {
            this.id = id;
            this.available = available;
        }

        @Override public String id() { return id; }
        @Override public boolean isAvailable() { return available; }
        @Override public boolean isFree() { return true; }
        @Override public List<String> supportedModels() { return List.of("test-model"); }
        @Override public String defaultModel() { return "test-model"; }
        @Override public ProviderCapabilities capabilities() { return ProviderCapabilities.standard(128000, 4096); }
        @Override public ProviderRateLimit rateLimit() { return ProviderRateLimit.of(30); }

        @Override
        public CompletableFuture<LlmResponse> chat(LlmRequest request) {
            if (failNext) {
                return CompletableFuture.failedFuture(new RuntimeException("simulated failure"));
            }
            return CompletableFuture.completedFuture(new LlmResponse(
                    id + "-response", "test-model", id, 10, 20, Duration.ofMillis(100), false, false));
        }
    }
}
