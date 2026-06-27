package dev.skymetron.application.scheduler;

import dev.skymetron.domain.observation.OperationMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class UserActivityTracker {

    private static final Logger log = LoggerFactory.getLogger(UserActivityTracker.class);
    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(30);
    private static final Duration SLEEP_TIMEOUT = Duration.ofHours(4);

    private final AtomicReference<Instant> lastActivity = new AtomicReference<>(Instant.now());
    private final AtomicReference<OperationMode> currentMode = new AtomicReference<>(OperationMode.IDLE);

    public void recordActivity() {
        lastActivity.set(Instant.now());
        OperationMode previous = currentMode.getAndSet(OperationMode.ACTIVE);
        if (previous != OperationMode.ACTIVE) {
            log.info("User activity detected -> mode: ACTIVE (was {})", previous);
        }
    }

    public OperationMode getCurrentMode() {
        return currentMode.get();
    }

    public void tick() {
        Instant now = Instant.now();
        Instant last = lastActivity.get();
        Duration elapsed = Duration.between(last, now);
        OperationMode mode = currentMode.get();

        OperationMode next;
        if (elapsed.compareTo(SLEEP_TIMEOUT) > 0) {
            next = OperationMode.SLEEP;
        } else if (elapsed.compareTo(IDLE_TIMEOUT) > 0) {
            next = OperationMode.IDLE;
        } else if (mode == OperationMode.DEEP) {
            next = OperationMode.DEEP;
        } else {
            next = OperationMode.ACTIVE;
        }

        if (next != mode) {
            currentMode.set(next);
            log.info("Mode transition: {} -> {} (last activity {} ago)", mode, next, elapsed);
        }
    }

    public void setMode(OperationMode mode) {
        OperationMode previous = currentMode.getAndSet(mode);
        if (previous != mode) {
            log.info("Manual mode override: {} -> {}", previous, mode);
        }
        if (mode == OperationMode.ACTIVE) {
            lastActivity.set(Instant.now());
        }
    }

    public Duration getIdleTime() {
        return Duration.between(lastActivity.get(), Instant.now());
    }
}
