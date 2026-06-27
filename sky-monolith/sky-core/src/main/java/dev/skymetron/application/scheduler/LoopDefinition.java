package dev.skymetron.application.scheduler;

import java.time.Duration;

/**
 * Definition of an autonomous background loop.
 */
public record LoopDefinition(
        String name,
        Duration frequency,
        Duration timeout,
        Duration minInterval,
        int maxConsecutiveFailures,
        Priority priority,
        ResourceBudget budget
) {
    public enum Priority { HIGH, MEDIUM, LOW, MINIMAL }

    public record ResourceBudget(
            int maxTokensPerRun,
            int maxDurationSeconds
    ) {
        public static ResourceBudget minimal() { return new ResourceBudget(0, 10); }
        public static ResourceBudget low() { return new ResourceBudget(500, 30); }
        public static ResourceBudget medium() { return new ResourceBudget(2000, 120); }
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String name;
        private Duration frequency = Duration.ofMinutes(5);
        private Duration timeout = Duration.ofSeconds(30);
        private Duration minInterval = Duration.ofMinutes(1);
        private int maxConsecutiveFailures = 3;
        private Priority priority = Priority.MEDIUM;
        private ResourceBudget budget = ResourceBudget.low();

        public Builder name(String n) { this.name = n; return this; }
        public Builder frequency(Duration f) { this.frequency = f; return this; }
        public Builder timeout(Duration t) { this.timeout = t; return this; }
        public Builder minInterval(Duration m) { this.minInterval = m; return this; }
        public Builder maxFailures(int f) { this.maxConsecutiveFailures = f; return this; }
        public Builder priority(Priority p) { this.priority = p; return this; }
        public Builder budget(ResourceBudget b) { this.budget = b; return this; }

        public LoopDefinition build() {
            return new LoopDefinition(name, frequency, timeout, minInterval,
                    maxConsecutiveFailures, priority, budget);
        }
    }
}
