package dev.skymetron.infrastructure.ai.provider;

import dev.skymetron.domain.tool.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Base class for OpenAI-compatible HTTP providers (Mistral, NVIDIA, Groq, OpenRouter).
 *
 * <p>All of these expose a {@code /chat/completions} endpoint with the same
 * JSON schema. Subclasses only configure the base URL, API key, model, and
 * capabilities.
 */
public abstract class OpenAiCompatibleProvider implements LlmProvider {

    protected final String apiKey;
    protected final String baseUrl;
    protected final String defaultModel;
    protected final ProviderCapabilities capabilities;
    protected final ProviderRateLimit rateLimit;
    protected final boolean enabled;

    protected OpenAiCompatibleProvider(String apiKey, String baseUrl, String defaultModel,
                                       ProviderCapabilities capabilities, ProviderRateLimit rateLimit,
                                       boolean enabled) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.defaultModel = defaultModel;
        this.capabilities = capabilities;
        this.rateLimit = rateLimit;
        this.enabled = enabled && apiKey != null && !apiKey.isBlank();
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }

    @Override
    public boolean isFree() {
        return true;
    }

    @Override
    public String defaultModel() {
        return defaultModel;
    }

    @Override
    public List<String> supportedModels() {
        return List.of(defaultModel);
    }

    @Override
    public ProviderCapabilities capabilities() {
        return capabilities;
    }

    @Override
    public ProviderRateLimit rateLimit() {
        return rateLimit;
    }

    /**
     * Build the OpenAI-compatible request body. Subclasses can override
     * to add provider-specific fields.
     */
    protected Map<String, Object> buildRequestBody(LlmRequest request) {
        List<Map<String, String>> msgs = request.messages().stream()
                .map(m -> Map.of("role", m.role().name().toLowerCase(), "content", m.content()))
                .toList();
        return Map.of(
                "model", request.model() != null ? request.model() : defaultModel,
                "messages", msgs,
                "temperature", request.temperature(),
                "max_tokens", request.maxTokens(),
                "stream", request.stream()
        );
    }
}
