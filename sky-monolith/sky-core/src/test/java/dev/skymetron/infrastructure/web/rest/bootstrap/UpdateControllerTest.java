package dev.skymetron.infrastructure.web.rest.bootstrap;

import dev.skymetron.application.update.UpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateController tests")
class UpdateControllerTest {

    @Mock
    UpdateService updateService;

    UpdateController controller;

    @BeforeEach
    void setUp() {
        controller = new UpdateController(updateService);
    }

    @Test
    @DisplayName("GET /check returns available when release found")
    void checkAvailable() {
        when(updateService.checkLatestRelease()).thenReturn(Optional.of(
            new UpdateService.ReleaseInfo("v0.2.0", "Release 0.2.0",
                "2026-06-28T12:00:00Z", "Notes", false)));

        ResponseEntity<?> response = controller.checkForUpdates();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("available", true);
        assertThat(body).containsEntry("tagName", "v0.2.0");
    }

    @Test
    @DisplayName("GET /check returns not available when no release found")
    void checkNotAvailable() {
        when(updateService.checkLatestRelease()).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.checkForUpdates();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("available", false);
    }
}
