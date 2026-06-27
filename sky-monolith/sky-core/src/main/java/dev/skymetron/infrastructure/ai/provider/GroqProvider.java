package dev.skymetron.infrastructure.ai.provider;

import dev.skymetron.domain.tool.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@Lazy
public class GroqProvider extends OpenAiCompatibleProvider {

    private final RestClient restClient;

    public GroqProvider(@Value("${sky.providers.groq.api-key:}") String apiKey,
                        @Value("${sky.providers.groq.enabled:false}") boolean enabled,
                        @Value("${sky.providers.groq.model:llama-3.3-70b-versatile}") String model) {
        super(apiKey, "https://api.groq.com/openai/v1", model,
                ProviderCapabilities.standard(128000, 8192),
                ProviderRateLimit.of(30), enabled);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String id() { return "groq"; }

    @Override
    public CompletableFuture<LlmResponse> chat(LlmRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            long start = java.lang.System.currentTimeMillis();
            ChatCompletionResponse resp = restClient.post()
                    .uri("/chat/completions")
                    .body(buildRequestBody(request))
                    .retrieve()
                    .body(ChatCompletionResponse.class);
            long elapsed = java.lang.System.currentTimeMillis() - start;
            return toResponse(resp, request, elapsed);
        });
    }

    private LlmResponse toResponse(ChatCompletionResponse resp, LlmRequest req, long elapsedMs) {
        String content = resp != null && resp.choices() != null && !resp.choices().isEmpty()
                ? resp.choices().get(0).message().content() : "";
        int promptTokens = resp != null && resp.usage() != null ? resp.usage().promptTokens() : 0;
        int completionTokens = resp != null && resp.usage() != null ? resp.usage().completionTokens() : 0;
        return new LlmResponse(content, req.model() != null ? req.model() : defaultModel,
                id(), promptTokens, completionTokens, Duration.ofMillis(elapsedMs), false, false);
    }
}
