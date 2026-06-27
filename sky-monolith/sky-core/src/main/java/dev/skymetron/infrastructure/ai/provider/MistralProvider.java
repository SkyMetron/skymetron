package dev.skymetron.infrastructure.ai.provider;

import dev.skymetron.domain.tool.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@Lazy
public class MistralProvider extends OpenAiCompatibleProvider {

    private final RestClient restClient;

    public MistralProvider(@Value("${sky.providers.mistral.api-key:}") String apiKey,
                           @Value("${sky.providers.mistral.enabled:false}") boolean enabled,
                           @Value("${sky.providers.mistral.model:mistral-large-latest}") String model) {
        super(apiKey, "https://api.mistral.ai/v1", model,
                ProviderCapabilities.standard(128000, 8192),
                ProviderRateLimit.of(20), enabled);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String id() { return "mistral"; }

    @Override
    public CompletableFuture<LlmResponse> chat(LlmRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            long start = java.lang.System.currentTimeMillis();
            Map<String, Object> body = buildRequestBody(request);
            ChatCompletionResponse resp = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
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
