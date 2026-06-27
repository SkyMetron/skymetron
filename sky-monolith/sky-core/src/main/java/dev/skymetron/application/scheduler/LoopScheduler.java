package dev.skymetron.application.scheduler;

import dev.skymetron.domain.observation.OperationMode;
import dev.skymetron.domain.observation.SystemOverloadEvent;
import dev.skymetron.domain.observation.SystemRecoveryEvent;
import dev.skymetron.infrastructure.metrics.SkyMetricsRegistry;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@EnableScheduling
public class LoopScheduler {

    private static final Logger log = LoggerFactory.getLogger(LoopScheduler.class);

    private final Map<String, LoopDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<String, LoopState> states = new ConcurrentHashMap<>();
    private final Map<String, Runnable> tasks = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastRunTimes = new ConcurrentHashMap<>();

    private final ExecutorService executor;
    private final AtomicBoolean systemOverloaded = new AtomicBoolean(false);
    private final SkyMetricsRegistry metrics;
    private final UserActivityTracker activityTracker;

    private final AtomicInteger loopBudgetCounter = new AtomicInteger(0);
    private static final int LOOP_BUDGET_MAX = 20;

    public LoopScheduler(SkyMetricsRegistry metrics, UserActivityTracker activityTracker) {
        this.metrics = metrics;
        this.activityTracker = activityTracker;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void register(LoopDefinition definition, Runnable task) {
        definitions.put(definition.name(), definition);
        states.put(definition.name(), LoopState.initial(definition.name()));
        tasks.put(definition.name(), task);
        log.info("Loop registered: {} frequency={} timeout={} priority={} budget={}",
                definition.name(), definition.frequency(), definition.timeout(), definition.priority(), definition.budget());
    }

    public void pause(String loopName) {
        LoopState state = states.get(loopName);
        if (state != null) {
            states.put(loopName, state.withPaused(true));
            log.info("Loop paused: {}", loopName);
        }
    }

    public void resume(String loopName) {
        LoopState state = states.get(loopName);
        if (state != null) {
            states.put(loopName, state.withPaused(false));
            log.info("Loop resumed: {}", loopName);
        }
    }

    public List<LoopState> getStatus() {
        return states.values().stream().toList();
    }

    public LoopState getStatus(String name) {
        return states.get(name);
    }

    @Scheduled(fixedDelay = 10000)
    public void tick() {
        activityTracker.tick();
        OperationMode mode = activityTracker.getCurrentMode();
        Instant now = Instant.now();

        for (var entry : definitions.entrySet()) {
            String name = entry.getKey();
            LoopDefinition def = entry.getValue();
            LoopState state = states.get(name);

            if (state == null || state.paused()) continue;
            if (state.running()) continue;
            if (state.consecutiveFailures() >= def.maxConsecutiveFailures()) {
                log.warn("Loop {} circuit breaker open ({} consecutive failures), skipping",
                        name, state.consecutiveFailures());
                continue;
            }
            if (systemOverloaded.get() && def.priority() == LoopDefinition.Priority.LOW) continue;
            if (!shouldRunInMode(mode, def)) continue;
            if (!respectBudget(def)) continue;

            Instant lastRun = lastRunTimes.get(name);
            if (lastRun != null && Duration.between(lastRun, now).compareTo(def.frequency()) < 0) {
                continue;
            }

            lastRunTimes.put(name, now);
            executor.submit(() -> runLoop(name, def));
        }
    }

    boolean shouldRunInMode(OperationMode mode, LoopDefinition def) {
        return switch (mode) {
            case ACTIVE, DEEP -> true;
            case IDLE -> def.priority() != LoopDefinition.Priority.LOW;
            case SLEEP -> def.priority() == LoopDefinition.Priority.HIGH;
        };
    }

    boolean respectBudget(LoopDefinition def) {
        if (def.priority() == LoopDefinition.Priority.HIGH || def.priority() == LoopDefinition.Priority.MINIMAL) {
            return true;
        }
        int current = loopBudgetCounter.incrementAndGet();
        if (current > LOOP_BUDGET_MAX) {
            if (current == LOOP_BUDGET_MAX + 1) {
                log.debug("Loop budget exhausted ({}), pausing MEDIUM/LOW loops until next tick", LOOP_BUDGET_MAX);
            }
            return current <= LOOP_BUDGET_MAX;
        }
        return true;
    }

    void resetBudgetForTesting() { loopBudgetCounter.set(0); }

    private void runLoop(String name, LoopDefinition def) {
        Instant start = Instant.now();
        LoopState state = states.get(name);
        states.put(name, state.withRunStart(start));
        log.info("Loop starting: {} (autonomous=true)", name);

        try {
            Runnable task = tasks.get(name);
            if (task != null) {
                task.run();
            }
            Instant end = Instant.now();
            long elapsedMs = Duration.between(start, end).toMillis();
            states.put(name, states.get(name).withRunSuccess(end));
            metrics.recordLoopExecution(name, true, elapsedMs, true);
            log.info("Loop completed: {} in {}ms (autonomous=true)", name, elapsedMs);
        } catch (Exception e) {
            Instant end = Instant.now();
            long elapsedMs = Duration.between(start, end).toMillis();
            LoopState failed = states.get(name).withRunFailure(end, e.getMessage());
            states.put(name, failed);
            metrics.recordLoopExecution(name, false, elapsedMs, true);
            metrics.recordLoopFailure(name);
            log.error("Loop failed: {} (consecutive failures={}/{}, autonomous=true)", name,
                    failed.consecutiveFailures(), def.maxConsecutiveFailures(), e);
        }
    }

    @EventListener(SystemOverloadEvent.class)
    public void onSystemOverload(SystemOverloadEvent event) {
        systemOverloaded.set(true);
        log.warn("System overload detected ({}) — pausing LOW priority loops", event.reason());
    }

    @EventListener(SystemRecoveryEvent.class)
    public void onSystemRecovery(SystemRecoveryEvent event) {
        systemOverloaded.set(false);
        log.info("System recovered ({}) — resuming LOW priority loops", event.reason());
    }

    @PreDestroy
    public void shutdown() {
        log.info("LoopScheduler shutting down...");
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
