package dev.skymetron.infrastructure.web.rest;

import dev.skymetron.application.scheduler.LoopScheduler;
import dev.skymetron.application.scheduler.LoopState;
import dev.skymetron.application.scheduler.UserActivityTracker;
import dev.skymetron.domain.observation.OperationMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LoopController tests")
class LoopControllerTest {

    @Mock
    LoopScheduler loopScheduler;

    @Mock
    UserActivityTracker activityTracker;

    LoopController controller;

    @BeforeEach
    void setUp() {
        controller = new LoopController(loopScheduler, activityTracker);
    }

    @Test
    @DisplayName("GET /api/loops/status returns mode and loops")
    void status() {
        when(activityTracker.getCurrentMode()).thenReturn(OperationMode.ACTIVE);
        when(activityTracker.getIdleTime()).thenReturn(Duration.ofMinutes(5));
        when(loopScheduler.getStatus()).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = controller.status();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("mode", "ACTIVE");
        assertThat(response.getBody()).containsKey("idleTime");
    }

    @Test
    @DisplayName("POST /api/loops/{name}/pause returns 200")
    void pause() {
        when(loopScheduler.getStatus("test-loop"))
                .thenReturn(new LoopState("test-loop", false, false, 0, null, null, null, 0, 0));

        ResponseEntity<Map<String, String>> response = controller.pause("test-loop");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "paused");
    }

    @Test
    @DisplayName("POST /api/loops/{name}/pause on non-existent returns 404")
    void pauseNotFound() {
        when(loopScheduler.getStatus("unknown")).thenReturn(null);

        ResponseEntity<Map<String, String>> response = controller.pause("unknown");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("POST /api/loops/{name}/resume returns 200")
    void resume() {
        when(loopScheduler.getStatus("test-loop"))
                .thenReturn(new LoopState("test-loop", false, true, 0, null, null, null, 0, 0));

        ResponseEntity<Map<String, String>> response = controller.resume("test-loop");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "resumed");
    }

    @Test
    @DisplayName("POST /api/loops/mode sets operation mode")
    void setMode() {
        ResponseEntity<Map<String, String>> response = controller.setMode(new ModeRequest("DEEP"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("mode", "DEEP");
    }

    @Test
    @DisplayName("POST /api/loops/mode with invalid mode returns 400")
    void setModeInvalid() {
        ResponseEntity<Map<String, String>> response = controller.setMode(new ModeRequest("INVALID"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("GET /api/loops/mode returns current mode")
    void getMode() {
        when(activityTracker.getCurrentMode()).thenReturn(OperationMode.DEEP);
        when(activityTracker.getIdleTime()).thenReturn(Duration.ofMinutes(30));

        ResponseEntity<Map<String, Object>> response = controller.getMode();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("mode", "DEEP");
    }
}
