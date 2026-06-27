package dev.skymetron.application.scheduler;

import dev.skymetron.application.agent.ConsolidationAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LoopRegistrar {

    private static final Logger log = LoggerFactory.getLogger(LoopRegistrar.class);

    private final LoopScheduler loopScheduler;
    private final ConsolidationAgent consolidationAgent;
    private final UserActivityTracker activityTracker;

    public LoopRegistrar(LoopScheduler loopScheduler, ConsolidationAgent consolidationAgent,
                         UserActivityTracker activityTracker) {
        this.loopScheduler = loopScheduler;
        this.consolidationAgent = consolidationAgent;
        this.activityTracker = activityTracker;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerLoops() {
        LoopDefinition healthCheck = LoopDefinition.builder()
                .name("health-check")
                .frequency(java.time.Duration.ofSeconds(10))
                .timeout(java.time.Duration.ofSeconds(5))
                .minInterval(java.time.Duration.ofSeconds(5))
                .maxFailures(10)
                .priority(LoopDefinition.Priority.HIGH)
                .budget(LoopDefinition.ResourceBudget.minimal())
                .build();

        loopScheduler.register(healthCheck, () -> {
            log.debug("Health check loop tick — system OK");
        });

        LoopDefinition consolidationLoop = LoopDefinition.builder()
                .name("memory-consolidation")
                .frequency(java.time.Duration.ofHours(4))
                .timeout(java.time.Duration.ofSeconds(30))
                .minInterval(java.time.Duration.ofMinutes(10))
                .maxFailures(3)
                .priority(LoopDefinition.Priority.MEDIUM)
                .budget(LoopDefinition.ResourceBudget.low())
                .build();

        loopScheduler.register(consolidationLoop, () -> {
            consolidationAgent.runConsolidation();
        });

        LoopDefinition researchBackground = LoopDefinition.builder()
                .name("research-background")
                .frequency(java.time.Duration.ofHours(1))
                .timeout(java.time.Duration.ofMinutes(5))
                .minInterval(java.time.Duration.ofMinutes(30))
                .maxFailures(5)
                .priority(LoopDefinition.Priority.LOW)
                .budget(LoopDefinition.ResourceBudget.medium())
                .build();

        loopScheduler.register(researchBackground, () -> {
            log.debug("Research background loop tick (opt-in)");
        });

        log.info("Registered {} autonomous loops (mode: {})", 3, activityTracker.getCurrentMode());
    }
}
