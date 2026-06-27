package dev.skymetron.infrastructure.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Central registry for all SkyMetron custom metrics.
 *
 * <p>Exposes Micrometer meters consumed by Prometheus via /actuator/prometheus.
 * All meters are tagged for rich Grafana dashboards.
 */
@Component
public class SkyMetricsRegistry {

    private final MeterRegistry meterRegistry;

    public SkyMetricsRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // ── Agent metrics ──

    public void recordAgentInvocation(String agentName, String intent, boolean success, long durationMs) {
        Counter.builder("sky.agent.invocations")
                .tag("agent", agentName)
                .tag("intent", intent)
                .tag("result", success ? "success" : "failure")
                .register(meterRegistry)
                .increment();

        Timer.builder("sky.agent.duration")
                .tag("agent", agentName)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordAgentError(String agentName, String errorType) {
        Counter.builder("sky.agent.errors")
                .tag("agent", agentName)
                .tag("type", errorType)
                .register(meterRegistry)
                .increment();
    }

    // ── Provider metrics ──

    public void recordProviderCall(String providerId, boolean success, long durationMs) {
        Counter.builder("sky.provider.calls")
                .tag("provider", providerId)
                .tag("result", success ? "success" : "failure")
                .register(meterRegistry)
                .increment();

        Timer.builder("sky.provider.duration")
                .tag("provider", providerId)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordProviderFallback(String failedProvider, String fallbackProvider) {
        Counter.builder("sky.provider.fallbacks")
                .tag("from", failedProvider)
                .tag("to", fallbackProvider)
                .register(meterRegistry)
                .increment();
    }

    public void setProviderAvailable(String providerId, boolean available) {
        Gauge.builder("sky.provider.available", () -> available ? 1.0 : 0.0)
                .tag("provider", providerId)
                .register(meterRegistry);
    }

    // ── Token metrics ──

    private final AtomicLong totalPromptTokens = new AtomicLong(0);
    private final AtomicLong totalCompletionTokens = new AtomicLong(0);

    public void recordTokenUsage(String providerId, int promptTokens, int completionTokens) {
        totalPromptTokens.addAndGet(promptTokens);
        totalCompletionTokens.addAndGet(completionTokens);

        Counter.builder("sky.tokens.prompt")
                .tag("provider", providerId)
                .register(meterRegistry)
                .increment(promptTokens);

        Counter.builder("sky.tokens.completion")
                .tag("provider", providerId)
                .register(meterRegistry)
                .increment(completionTokens);
    }

    // ── Memory metrics ──

    public void recordMemoryStored(String memoryType) {
        Counter.builder("sky.memory.stored")
                .tag("type", memoryType)
                .register(meterRegistry)
                .increment();
    }

    public void recordMemoryMerged() {
        Counter.builder("sky.memory.merged").register(meterRegistry).increment();
    }

    public void setMemoryCount(long count) {
        Gauge.builder("sky.memory.count", () -> count)
                .register(meterRegistry);
    }

    // ── Loop metrics ──

    public void recordLoopExecution(String loopName, boolean success, long durationMs, boolean autonomous) {
        Counter.builder("sky.loop.executions")
                .tag("loop", loopName)
                .tag("result", success ? "success" : "failure")
                .tag("autonomous", String.valueOf(autonomous))
                .register(meterRegistry)
                .increment();

        Timer.builder("sky.loop.duration")
                .tag("loop", loopName)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordLoopFailure(String loopName) {
        Counter.builder("sky.loop.failures")
                .tag("loop", loopName)
                .register(meterRegistry)
                .increment();
    }

    // ── Trace metrics ──

    public void recordEventConsumed(String eventType, boolean success) {
        Counter.builder("sky.trace.events")
                .tag("type", eventType)
                .tag("result", success ? "success" : "failure")
                .register(meterRegistry)
                .increment();
    }

    // ── Cache metrics ──

    public void recordCacheHit(String cacheName) {
        Counter.builder("sky.cache.hits")
                .tag("cache", cacheName)
                .register(meterRegistry)
                .increment();
    }

    public void recordCacheMiss(String cacheName) {
        Counter.builder("sky.cache.misses")
                .tag("cache", cacheName)
                .register(meterRegistry)
                .increment();
    }

    // ── Embedding metrics ──

    public void recordEmbedding(boolean batch, long durationMs) {
        Counter.builder("sky.embeddings")
                .tag("mode", batch ? "batch" : "single")
                .tag("result", "success")
                .register(meterRegistry)
                .increment();

        Timer.builder("sky.embedding.duration")
                .tag("mode", batch ? "batch" : "single")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    // ── Tool metrics ──

    public void recordToolExecution(String toolName, String action, boolean success, long durationMs) {
        Counter.builder("sky.tool.executions")
                .tag("tool", toolName)
                .tag("action", action)
                .tag("result", success ? "success" : "failure")
                .register(meterRegistry)
                .increment();

        Timer.builder("sky.tool.duration")
                .tag("tool", toolName)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
}
