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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoopScheduler tests")
class LoopSchedulerTest {

    @Mock
    SkyMetricsRegistry metrics;

    LoopScheduler scheduler;
    UserActivityTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new UserActivityTracker();
        scheduler = new LoopScheduler(metrics, tracker);
    }

    @Test
    @DisplayName("register() adds a loop definition")
    void register() {
        LoopDefinition def = LoopDefinition.builder()
                .name("test-loop")
                .frequency(Duration.ofSeconds(10))
                .maxFailures(3)
                .priority(LoopDefinition.Priority.MEDIUM)
                .build();
        scheduler.register(def, () -> {});
        assertThat(scheduler.getStatus()).hasSize(1);
    }

    @Test
    @DisplayName("pause() marks loop as paused")
    void pause() {
        registerTestLoop("test", LoopDefinition.Priority.MEDIUM);
        scheduler.pause("test");
        LoopState state = scheduler.getStatus("test");
        assertThat(state).isNotNull();
        assertThat(state.paused()).isTrue();
    }

    @Test
    @DisplayName("resume() marks loop as active")
    void resume() {
        registerTestLoop("test", LoopDefinition.Priority.MEDIUM);
        scheduler.pause("test");
        scheduler.resume("test");
        assertThat(scheduler.getStatus("test").paused()).isFalse();
    }

    @Test
    @DisplayName("getStatus returns all registered loops")
    void getStatus() {
        registerTestLoop("loop-a", LoopDefinition.Priority.HIGH);
        registerTestLoop("loop-b", LoopDefinition.Priority.LOW);
        List<LoopState> states = scheduler.getStatus();
        assertThat(states).hasSize(2);
    }

    @Test
    @DisplayName("shouldRunInMode allows HIGH in SLEEP mode")
    void shouldRunInModeHighInSleep() {
        LoopDefinition def = LoopDefinition.builder().name("h").priority(LoopDefinition.Priority.HIGH).build();
        assertThat(scheduler.shouldRunInMode(OperationMode.SLEEP, def)).isTrue();
    }

    @Test
    @DisplayName("shouldRunInMode blocks LOW in SLEEP mode")
    void shouldRunInModeLowInSleep() {
        LoopDefinition def = LoopDefinition.builder().name("l").priority(LoopDefinition.Priority.LOW).build();
        assertThat(scheduler.shouldRunInMode(OperationMode.SLEEP, def)).isFalse();
    }

    @Test
    @DisplayName("shouldRunInMode blocks LOW in IDLE mode")
    void shouldRunInModeLowInIdle() {
        LoopDefinition def = LoopDefinition.builder().name("l").priority(LoopDefinition.Priority.LOW).build();
        assertThat(scheduler.shouldRunInMode(OperationMode.IDLE, def)).isFalse();
    }

    @Test
    @DisplayName("shouldRunInMode allows MEDIUM in IDLE mode")
    void shouldRunInModeMediumInIdle() {
        LoopDefinition def = LoopDefinition.builder().name("m").priority(LoopDefinition.Priority.MEDIUM).build();
        assertThat(scheduler.shouldRunInMode(OperationMode.IDLE, def)).isTrue();
    }

    @Test
    @DisplayName("shouldRunInMode allows all in ACTIVE mode")
    void shouldRunInModeAllInActive() {
        LoopDefinition def = LoopDefinition.builder().name("l").priority(LoopDefinition.Priority.LOW).build();
        assertThat(scheduler.shouldRunInMode(OperationMode.ACTIVE, def)).isTrue();
    }

    @Test
    @DisplayName("shouldRunInMode allows all in DEEP mode")
    void shouldRunInModeAllInDeep() {
        LoopDefinition def = LoopDefinition.builder().name("l").priority(LoopDefinition.Priority.LOW).build();
        assertThat(scheduler.shouldRunInMode(OperationMode.DEEP, def)).isTrue();
    }

    @Test
    @DisplayName("respectBudget allows HIGH priority regardless of counter")
    void respectBudgetHigh() {
        LoopDefinition def = LoopDefinition.builder()
                .name("h").priority(LoopDefinition.Priority.HIGH).build();
        assertThat(scheduler.respectBudget(def)).isTrue();
    }

    @Test
    @DisplayName("respectBudget blocks MEDIUM/LOW after budget exhausted")
    void respectBudgetExhaustion() {
        scheduler.resetBudgetForTesting();
        LoopDefinition def = LoopDefinition.builder()
                .name("m").priority(LoopDefinition.Priority.MEDIUM).build();
        for (int i = 0; i < LoopSchedulerTestHelper.getBudgetMax(); i++) {
            scheduler.respectBudget(def);
        }
        assertThat(scheduler.respectBudget(def)).isFalse();
    }

    @Test
    @DisplayName("pause on non-existent loop does nothing")
    void pauseNonExistent() {
        scheduler.pause("nonexistent");
        assertThat(scheduler.getStatus("nonexistent")).isNull();
    }

    @Test
    @DisplayName("tick does not run paused loops")
    void tickSkipsPaused() {
        AtomicInteger counter = new AtomicInteger(0);
        LoopDefinition def = LoopDefinition.builder()
                .name("paused-test")
                .frequency(Duration.ofSeconds(1))
                .maxFailures(3)
                .priority(LoopDefinition.Priority.HIGH)
                .build();
        scheduler.register(def, counter::incrementAndGet);
        scheduler.pause("paused-test");
        scheduler.resetBudgetForTesting();
        scheduler.tick();
        assertThat(counter.get()).isEqualTo(0);
    }

    private void registerTestLoop(String name, LoopDefinition.Priority priority) {
        LoopDefinition def = LoopDefinition.builder()
                .name(name)
                .frequency(Duration.ofSeconds(10))
                .maxFailures(3)
                .priority(priority)
                .build();
        scheduler.register(def, () -> {});
    }
}

class LoopSchedulerTestHelper {
    static int getBudgetMax() { return 20; }
}
