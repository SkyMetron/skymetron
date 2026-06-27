package dev.skymetron.application.provider;

import dev.skymetron.domain.tool.LlmProvider;
import dev.skymetron.domain.tool.LlmRequest;
import dev.skymetron.domain.tool.LlmResponse;
import dev.skymetron.domain.tool.TaskType;
import dev.skymetron.domain.observation.ProviderFallbackEvent;
import dev.skymetron.infrastructure.messaging.EventPublisher;
import dev.skymetron.infrastructure.metrics.SkyMetricsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of all LLM providers. Routes requests to the best provider for a
 * given {@link TaskType} and implements the global fallback chain.
 *
 * <p>Routing table (per ADR-003): each TaskType has an ordered preference
 * list. If the preferred provider fails (timeout, rate-limit, error), the
 * registry falls back to the next in the chain.
 */
@Component
public class ProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(ProviderRegistry.class);

    private final Map<String, LlmProvider> providers = new ConcurrentHashMap<>();
    private final Map<String, ProviderStatus> statuses = new ConcurrentHashMap<>();
    private final List<String> fallbackChain;
    private final EventPublisher eventPublisher;
    private final SkyMetricsRegistry metrics;

    public ProviderRegistry(List<LlmProvider> providerList, EventPublisher eventPublisher, SkyMetricsRegistry metrics) {
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
        for (LlmProvider p : providerList) {
            providers.put(p.id(), p);
            statuses.put(p.id(), new ProviderStatus(p.id(), p.isAvailable(), p.isFree(), null, null, null));
        }
        this.fallbackChain = List.of("mistral", "nvidia", "google", "groq", "cerebras", "openrouter", "ollama-local");
        log.info("ProviderRegistry initialized with {} providers: {}", providers.size(), providers.keySet());
    }

    /**
     * Route a request to the best provider for the task type, with fallback.
     *
     * @return the response from the first successful provider
     * @throws ProviderUnavailableException if all providers fail
     */
    public CompletableFuture<LlmResponse> chat(LlmRequest request, TaskType taskType) {
        List<String> ordered = routingOrder(taskType);
        log.debug("Routing taskType={} via providers: {}", taskType, ordered);
        return tryChain(request, ordered, 0, false);
    }

    /**
     * Chat using the explicit fallback chain (no task-type routing).
     */
    public CompletableFuture<LlmResponse> chatWithFallback(LlmRequest request) {
        return tryChain(request, fallbackChain, 0, false);
    }

    private CompletableFuture<LlmResponse> tryChain(LlmRequest request, List<String> chain, int index, boolean fromFallback) {
        if (index >= chain.size()) {
            return CompletableFuture.failedFuture(
                    new ProviderUnavailableException("All providers exhausted. Last chain: " + chain));
        }
        String providerId = chain.get(index);
        LlmProvider provider = providers.get(providerId);
        if (provider == null || !provider.isAvailable() || !statuses.get(providerId).available()) {
            log.debug("Skipping provider {} (not available)", providerId);
            return tryChain(request, chain, index + 1, true);
        }
        Instant start = Instant.now();
        return provider.chat(request)
                .thenApply(resp -> {
                    long ms = Duration.between(start, Instant.now()).toMillis();
                    markSuccess(providerId);
                    metrics.recordProviderCall(providerId, true, ms);
                    metrics.recordTokenUsage(providerId, resp.promptTokens(), resp.completionTokens());
                    metrics.setProviderAvailable(providerId, true);
                    LlmResponse tagged = fromFallback ? resp.withFromFallback(true) : resp;
                    log.info("Provider {} responded in {}ms (fallback={})", providerId, ms, fromFallback);
                    return tagged;
                })
                .exceptionally(ex -> {
                    log.warn("Provider {} failed: {}", providerId, ex.getMessage());
                    markFailure(providerId, ex);
                    metrics.recordProviderCall(providerId, false, 0);
                    metrics.setProviderAvailable(providerId, false);
                    if (fromFallback == false && index + 1 < chain.size()) {
                        eventPublisher.publishProviderFallback(ProviderFallbackEvent.of(
                                providerId, chain.get(index + 1), ex.getMessage(), java.util.UUID.randomUUID()));
                        metrics.recordProviderFallback(providerId, chain.get(index + 1));
                    }
                    try {
                        return tryChain(request, chain, index + 1, true).join();
                    } catch (Exception e) {
                        throw new ProviderUnavailableException("Provider " + providerId + " failed and fallback also exhausted", e);
                    }
                });
    }

    /**
     * Ordered preference list for a task type (per ADR-003 routing table).
     */
    public List<String> routingOrder(TaskType taskType) {
        return switch (taskType) {
            case CODE_GENERATION, REFACTORING, QA_TESTING, CODE_RESEARCH -> List.of("mistral", "nvidia", "google", "groq", "cerebras", "openrouter", "ollama-local");
            case LONG_ANALYSIS, WEB_RESEARCH, DOCUMENTATION, DOCS_RESEARCH, PAPER_RESEARCH -> List.of("google", "mistral", "nvidia", "groq", "cerebras", "openrouter", "ollama-local");
            case FAST_RESPONSE -> List.of("groq", "cerebras", "ollama-local", "nvidia", "openrouter", "mistral");
            case LONG_CONTEXT -> List.of("google", "mistral", "nvidia", "ollama-local");
            case OFFLINE -> List.of("ollama-local");
            case SECURITY -> List.of("nvidia", "mistral", "google", "groq", "cerebras", "openrouter", "ollama-local");
            case MEMORY_DEDUP -> List.of("groq", "cerebras", "ollama-local", "openrouter", "mistral", "nvidia", "google");
            case VAULT_CONSOLIDATION -> List.of("ollama-local", "groq", "cerebras", "openrouter", "mistral", "nvidia", "google");
            case GENERAL -> fallbackChain;
        };
    }

    public List<ProviderStatus> getStatus() {
        return fallbackChain.stream()
                .map(id -> statuses.getOrDefault(id, ProviderStatus.unknown(id)))
                .toList();
    }

    public Optional<LlmProvider> getProvider(String id) {
        return Optional.ofNullable(providers.get(id));
    }

    public List<String> getFallbackChain() {
        return fallbackChain;
    }

    private void markSuccess(String providerId) {
        ProviderStatus current = statuses.get(providerId);
        statuses.put(providerId, current.withAvailable(true).withLastSuccess(Instant.now()));
    }

    private void markFailure(String providerId, Throwable ex) {
        ProviderStatus current = statuses.get(providerId);
        statuses.put(providerId, current.withAvailable(false).withLastError(ex.getMessage()).withLastFailure(Instant.now()));
    }

    public record ProviderStatus(
            String providerId,
            boolean available,
            boolean free,
            Instant lastSuccess,
            Instant lastFailure,
            String lastError
    ) {
        static ProviderStatus unknown(String id) {
            return new ProviderStatus(id, false, true, null, null, null);
        }
        ProviderStatus withAvailable(boolean a) { return new ProviderStatus(providerId, a, free, lastSuccess, lastFailure, lastError); }
        ProviderStatus withLastSuccess(Instant t) { return new ProviderStatus(providerId, true, free, t, lastFailure, lastError); }
        ProviderStatus withLastFailure(Instant t) { return new ProviderStatus(providerId, available, free, lastSuccess, t, lastError); }
        ProviderStatus withLastError(String e) { return new ProviderStatus(providerId, available, free, lastSuccess, lastFailure, e); }
    }

    public static class ProviderUnavailableException extends RuntimeException {
        public ProviderUnavailableException(String message) { super(message); }
        public ProviderUnavailableException(String message, Throwable cause) { super(message, cause); }
    }
}
