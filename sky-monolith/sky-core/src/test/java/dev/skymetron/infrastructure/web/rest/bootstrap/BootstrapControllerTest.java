package dev.skymetron.infrastructure.web.rest.bootstrap;

import dev.skymetron.application.bootstrap.ConfigService;
import dev.skymetron.application.bootstrap.VaultBootstrapService;
import dev.skymetron.application.bootstrap.WorkspaceBootstrapService;
import dev.skymetron.application.bootstrap.WorkspaceConfig;
import dev.skymetron.application.privacy.PrivacyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BootstrapController tests")
class BootstrapControllerTest {

    @Mock
    WorkspaceBootstrapService workspaceBootstrapService;
    @Mock
    VaultBootstrapService vaultBootstrapService;
    @Mock
    ConfigService configService;
    @Mock
    PrivacyService privacyService;

    BootstrapController controller;

    @BeforeEach
    void setUp() {
        controller = new BootstrapController(
            workspaceBootstrapService, vaultBootstrapService,
            configService, privacyService);
    }

    @Test
    @DisplayName("GET /status returns workspace config")
    void getStatus() throws Exception {
        var config = new WorkspaceConfig(
            "test-user", "USER",
            Path.of("/home/test/SkyMetron"), Path.of("/home/test/SkyMetron/vault"),
            Path.of("/home/test/.skymetron/config.json"),
            List.of(), false, false, false,
            false, false, false, false, false);
        when(workspaceBootstrapService.buildConfig(anyString(), anyBoolean()))
            .thenReturn(config);

        ResponseEntity<?> response = controller.getStatus("test-user");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKey("workspaceType");
        assertThat(body).containsKey("dockerDetected");
        assertThat(body).containsKey("isComplete");
    }

    @Test
    @DisplayName("POST /workspace creates workspace")
    void createWorkspace() throws Exception {
        var config = new WorkspaceConfig(
            "test-user", "USER",
            Path.of("/home/test/SkyMetron"), Path.of("/home/test/SkyMetron/vault"),
            Path.of("/home/test/.skymetron/config.json"),
            List.of(), false, false, false,
            false, false, false, false, false);
        when(workspaceBootstrapService.buildConfig(anyString(), anyBoolean()))
            .thenReturn(config);

        ResponseEntity<?> response = controller.createWorkspace("test-user");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("status", "created");
    }

    @Test
    @DisplayName("POST /vault/scan returns vault scan result")
    void scanVault() throws Exception {
        var config = new WorkspaceConfig(
            "test-user", "USER",
            Path.of("/home/test/SkyMetron"), Path.of("/home/test/SkyMetron/vault"),
            Path.of("/home/test/.skymetron/config.json"),
            List.of(), false, false, false,
            false, false, false, false, false);
        when(workspaceBootstrapService.buildConfig(anyString(), anyBoolean()))
            .thenReturn(config);
        when(vaultBootstrapService.scan(any(Path.class)))
            .thenReturn(new VaultBootstrapService.VaultBootstrapResult(5, 10, 1024, true, null));

        ResponseEntity<?> response = controller.scanVault("test-user");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("markdownFiles", 5L);
    }

    @Test
    @DisplayName("POST /accept-terms accepts terms")
    void acceptTerms() throws Exception {
        ResponseEntity<?> response = controller.acceptTerms();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("accepted", true);
    }

    @Test
    @DisplayName("POST /accept-lgpd accepts LGPD")
    void acceptLgpd() throws Exception {
        ResponseEntity<?> response = controller.acceptLgpd();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("accepted", true);
    }

    @Test
    @DisplayName("GET /legal-status returns acceptance status")
    void legalStatus() throws Exception {
        when(privacyService.hasAcceptedTerms()).thenReturn(true);
        when(privacyService.hasAcceptedLgpd()).thenReturn(false);

        ResponseEntity<?> response = controller.legalStatus();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("termsAccepted", true);
        assertThat(body).containsEntry("lgpdAccepted", false);
    }
}
