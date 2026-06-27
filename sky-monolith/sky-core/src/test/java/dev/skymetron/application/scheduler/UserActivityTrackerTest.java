package dev.skymetron.application.scheduler;

import dev.skymetron.domain.observation.OperationMode;
import dev.skymetron.infrastructure.metrics.SkyMetricsRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserActivityTracker tests")
class UserActivityTrackerTest {

    @Mock
    SkyMetricsRegistry metrics;

    UserActivityTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new UserActivityTracker();
    }

    @Test
    @DisplayName("initial mode is IDLE")
    void initialMode() {
        assertThat(tracker.getCurrentMode()).isEqualTo(OperationMode.IDLE);
    }

    @Test
    @DisplayName("recordActivity() sets mode to ACTIVE")
    void recordActivity() {
        tracker.recordActivity();
        assertThat(tracker.getCurrentMode()).isEqualTo(OperationMode.ACTIVE);
    }

    @Test
    @DisplayName("setMode() overrides current mode")
    void setMode() {
        tracker.setMode(OperationMode.DEEP);
        assertThat(tracker.getCurrentMode()).isEqualTo(OperationMode.DEEP);
    }

    @Test
    @DisplayName("setMode(ACTIVE) records activity timestamp")
    void setModeActiveRecordsActivity() {
        tracker.setMode(OperationMode.SLEEP);
        assertThat(tracker.getCurrentMode()).isEqualTo(OperationMode.SLEEP);
        tracker.setMode(OperationMode.ACTIVE);
        assertThat(tracker.getCurrentMode()).isEqualTo(OperationMode.ACTIVE);
        assertThat(tracker.getIdleTime()).isLessThan(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("getIdleTime() returns small value after recordActivity")
    void idleTimeAfterActivity() {
        tracker.recordActivity();
        assertThat(tracker.getIdleTime()).isLessThan(Duration.ofSeconds(5));
    }
}
