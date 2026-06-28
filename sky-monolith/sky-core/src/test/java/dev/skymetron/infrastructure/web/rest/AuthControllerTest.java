package dev.skymetron.infrastructure.web.rest;

import dev.skymetron.application.bootstrap.WorkspaceBootstrapService;
import dev.skymetron.infrastructure.security.JwtTokenProvider;
import dev.skymetron.infrastructure.security.github.GitHubAuthService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController tests")
class AuthControllerTest {

    @Mock
    GitHubAuthService gitHubAuthService;
    @Mock
    JwtTokenProvider tokenProvider;
    @Mock
    WorkspaceBootstrapService workspaceBootstrapService;

    AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(gitHubAuthService, tokenProvider, workspaceBootstrapService);
    }

    @Test
    @DisplayName("GET /github/url returns OAuth URL")
    void getGitHubUrl() {
        when(gitHubAuthService.getAuthorizationUrl(anyString()))
            .thenReturn("https://github.com/login/oauth/authorize?client_id=test");

        ResponseEntity<?> response = controller.getGitHubUrl("http://localhost:3000/callback");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKey("url");
        assertThat(body.get("url")).asString().startsWith("https://github.com/login/oauth/authorize");
    }

    @Test
    @DisplayName("GET /me returns username")
    void me() {
        ResponseEntity<?> response = controller.me("test-user");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("username", "test-user");
    }

    @Test
    @DisplayName("GET /me returns empty string when no attribute")
    void meNoAttribute() {
        ResponseEntity<?> response = controller.me(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("username", "");
    }
}
