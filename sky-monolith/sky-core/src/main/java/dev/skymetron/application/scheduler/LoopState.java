package dev.skymetron.application.scheduler;

import java.time.Instant;

/**
 * Runtime state of a registered loop.
 */
public record LoopState(
        String name,
        boolean running,
        boolean paused,
        int consecutiveFailures,
        Instant lastRunStart,
        Instant lastRunEnd,
        String lastError,
        long totalRuns,
        long successfulRuns
) {
    static LoopState initial(String name) {
        return new LoopState(name, false, false, 0, null, null, null, 0, 0);
    }

    LoopState withRunStart(Instant t) {
        return new LoopState(name, true, paused, consecutiveFailures, t, lastRunEnd, lastError, totalRuns + 1, successfulRuns);
    }

    LoopState withRunSuccess(Instant t) {
        return new LoopState(name, false, paused, 0, lastRunStart, t, null, totalRuns, successfulRuns + 1);
    }

    LoopState withRunFailure(Instant t, String error) {
        return new LoopState(name, false, paused, consecutiveFailures + 1, lastRunStart, t, error, totalRuns, successfulRuns);
    }

    LoopState withPaused(boolean p) {
        return new LoopState(name, false, p, consecutiveFailures, lastRunStart, lastRunEnd, lastError, totalRuns, successfulRuns);
    }
}
