package dev.skymetron.infrastructure.web.rest.bootstrap;

import dev.skymetron.application.privacy.PrivacyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrivacyController tests")
class PrivacyControllerTest {

    @Mock
    PrivacyService privacyService;

    PrivacyController controller;

    @BeforeEach
    void setUp() {
        controller = new PrivacyController(privacyService);
    }

    @Test
    @DisplayName("POST /export returns export result")
    void exportData() throws Exception {
        when(privacyService.exportData())
            .thenReturn(new PrivacyService.ExportResult("/tmp/export", 1024L));

        ResponseEntity<?> response = controller.exportData();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("path", "/tmp/export");
        assertThat(body).containsEntry("sizeBytes", 1024L);
    }

    @Test
    @DisplayName("POST /export returns 500 on failure")
    void exportDataError() throws Exception {
        when(privacyService.exportData()).thenThrow(new RuntimeException("Export error"));

        ResponseEntity<?> response = controller.exportData();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("DELETE /account deletes account")
    void deleteAccount() throws Exception {
        ResponseEntity<?> response = controller.deleteAccount();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("status", "deleted");
    }

    @Test
    @DisplayName("DELETE /account returns 500 on failure")
    void deleteAccountError() throws Exception {
        doThrow(new RuntimeException("Delete error")).when(privacyService).deleteLocalAccount();

        ResponseEntity<?> response = controller.deleteAccount();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("POST /clear-cache clears cache")
    void clearCache() throws Exception {
        ResponseEntity<?> response = controller.clearCache();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("status", "cleared");
    }

    @Test
    @DisplayName("POST /clear-cache returns 500 on failure")
    void clearCacheError() throws Exception {
        doThrow(new RuntimeException("Clear error")).when(privacyService).clearCache();

        ResponseEntity<?> response = controller.clearCache();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
