package dev.skymetron.infrastructure.ai.provider;

import dev.skymetron.domain.tool.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Google Gemini provider — uses the Gemini REST API (not OpenAI-compatible).
 */
@Component
@Lazy
public class GoogleProvider implements LlmProvider {

    private final String apiKey;
    private final String model;
    private final boolean enabled;
    private final RestClient restClient;

    public GoogleProvider(@Value("${sky.providers.google.api-key:}") String apiKey,
                          @Value("${sky.providers.google.enabled:false}") boolean enabled,
                          @Value("${sky.providers.google.model:gemini-2.0-flash}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.enabled = enabled && apiKey != null && !apiKey.isBlank();
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    @Override
    public String id() { return "google"; }

    @Override
    public boolean isAvailable() { return enabled; }

    @Override
    public boolean isFree() { return true; }

    @Override
    public String defaultModel() { return model; }

    @Override
    public List<String> supportedModels() { return List.of(model); }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(1_000_000, 8192, true, true, true);
    }

    @Override
    public ProviderRateLimit rateLimit() { return ProviderRateLimit.of(15); }

    @Override
    public CompletableFuture<LlmResponse> chat(LlmRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            long start = java.lang.System.currentTimeMillis();
            List<Map<String, Object>> contents = new ArrayList<>();
            for (Message msg : request.messages()) {
                String role = msg.role() == Message.Role.ASSISTANT ? "model" : "user";
                if (msg.role() == Message.Role.SYSTEM) {
                    contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", msg.content()))));
                } else {
                    contents.add(Map.of("role", role, "parts", List.of(Map.of("text", msg.content()))));
                }
            }
            Map<String, Object> body = Map.of("contents", contents);
            GeminiResponse resp = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .body(body)
                    .retrieve()
                    .body(GeminiResponse.class);
            long elapsed = java.lang.System.currentTimeMillis() - start;
            String content = extractContent(resp);
            return new LlmResponse(content, model, id(), 0, 0, Duration.ofMillis(elapsed), false, false);
        });
    }

    private String extractContent(GeminiResponse resp) {
        if (resp == null || resp.candidates() == null || resp.candidates().isEmpty()) return "";
        var parts = resp.candidates().get(0).content().parts();
        if (parts == null || parts.isEmpty()) return "";
        return parts.get(0).text();
    }

    record GeminiResponse(List<Candidate> candidates, UsageMetadata usageMetadata) {
        record Candidate(Content content) {}
        record Content(List<Part> parts) {}
        record Part(String text) {}
        record UsageMetadata(int promptTokenCount, int candidatesTokenCount) {}
    }
}
