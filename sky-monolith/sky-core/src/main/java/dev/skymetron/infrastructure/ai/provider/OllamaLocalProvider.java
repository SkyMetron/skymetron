package dev.skymetron.infrastructure.ai.provider;

import dev.skymetron.domain.tool.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Ollama local provider — runs models on the user's hardware.
 *
 * <p>Always free, always available if Ollama is running. No rate limits.
 * Used as the last-resort fallback and for offline mode.
 */
@Component
@Lazy
public class OllamaLocalProvider implements LlmProvider {

    private final String baseUrl;
    private final String model;
    private final RestClient restClient;

    public OllamaLocalProvider(@Value("${sky.ollama.base-url:http://localhost:11434}") String baseUrl,
                               @Value("${sky.ollama.chat-model:llama3.1}") String model) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public String id() { return "ollama-local"; }

    @Override
    public boolean isAvailable() {
        try {
            restClient.get().uri("/api/tags").retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isFree() { return true; }

    @Override
    public String defaultModel() { return model; }

    @Override
    public List<String> supportedModels() { return List.of(model); }

    @Override
    public ProviderCapabilities capabilities() {
        return ProviderCapabilities.standard(128000, 4096);
    }

    @Override
    public ProviderRateLimit rateLimit() {
        return new ProviderRateLimit(Integer.MAX_VALUE, Duration.ZERO);
    }

    @Override
    public CompletableFuture<LlmResponse> chat(LlmRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            long start = java.lang.System.currentTimeMillis();
            List<Map<String, String>> msgs = request.messages().stream()
                    .map(m -> Map.of("role", m.role().name().toLowerCase(), "content", m.content()))
                    .toList();
            Map<String, Object> body = Map.of(
                    "model", request.model() != null ? request.model() : model,
                    "messages", msgs,
                    "stream", false,
                    "options", Map.of("temperature", request.temperature())
            );
            OllamaChatResponse resp = restClient.post()
                    .uri("/api/chat")
                    .body(body)
                    .retrieve()
                    .body(OllamaChatResponse.class);
            long elapsed = java.lang.System.currentTimeMillis() - start;
            String content = resp != null ? resp.message().content() : "";
            int promptTokens = resp != null && resp.promptEvalCount() != null ? resp.promptEvalCount() : 0;
            int completionTokens = resp != null && resp.evalCount() != null ? resp.evalCount() : 0;
            return new LlmResponse(content, model, id(), promptTokens, completionTokens,
                    Duration.ofMillis(elapsed), false, false);
        });
    }

    record OllamaChatResponse(String model, Message message, Boolean done,
                              Integer promptEvalCount, Integer evalCount) {
        record Message(String role, String content) {}
    }
}
