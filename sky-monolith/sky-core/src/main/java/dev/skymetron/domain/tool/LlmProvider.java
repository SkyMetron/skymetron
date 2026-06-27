package dev.skymetron.domain.tool;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A single LLM provider (Mistral, NVIDIA, Gemini, Groq, OpenRouter, Ollama).
 *
 * <p>Each implementation calls one provider's API and reports its availability
 * and rate-limit status to the {@code ProviderRegistry}.
 */
public interface LlmProvider {

    /** Stable identifier (e.g. "mistral", "nvidia", "ollama-local"). */
    String id();

    /** Whether the provider is currently reachable and configured. */
    boolean isAvailable();

    /** Whether the provider is free (always true except OpenCode Go). */
    boolean isFree();

    /** Models this provider can serve. */
    List<String> supportedModels();

    /** The default model to use when none is specified. */
    String defaultModel();

    /** Execute a chat completion request. */
    CompletableFuture<LlmResponse> chat(LlmRequest request);

    ProviderCapabilities capabilities();

    ProviderRateLimit rateLimit();
}
