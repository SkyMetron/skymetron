package dev.skymetron.application.agent;

import dev.skymetron.domain.execution.*;
import dev.skymetron.infrastructure.messaging.EventPublisher;
import dev.skymetron.infrastructure.metrics.SkyMetricsRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SecurityAgent tests")
class SecurityAgentTest {

    @Mock
    EventPublisher eventPublisher;
    @Mock
    SkyMetricsRegistry metrics;

    SecurityAgent securityAgent;
    UUID traceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        securityAgent = new SecurityAgent(eventPublisher, metrics);
    }

    @Test
    @DisplayName("handleSecurityRequest() scans clean code")
    void scanCleanCode() {
        String code = "public class Hello { public void greet() { System.out.println(\"hi\"); } }";
        String result = securityAgent.handleSecurityRequest("scan|Hello|" + code, traceId);
        assertThat(result).contains("PASSED");
    }

    @Test
    @DisplayName("handleSecurityRequest() detects SQL Injection")
    void detectSqlInjection() {
        String code = "public void login() { stmt.executeQuery(\"SELECT * FROM users\"); }";
        String result = securityAgent.handleSecurityRequest("scan|Login|" + code, traceId);
        assertThat(result).contains("FAILED");
        assertThat(result).contains("SQL Injection");
    }

    @Test
    @DisplayName("handleSecurityRequest() detects hardcoded secrets")
    void detectHardcodedSecret() {
        String code = "password=admin123\napiKey=sk-abc";
        String result = securityAgent.handleSecurityRequest("scan|Config|" + code, traceId);
        assertThat(result).contains("FAILED");
        assertThat(result).contains("Hardcoded Secret");
    }

    @Test
    @DisplayName("handleSecurityRequest() returns status")
    void status() {
        String result = securityAgent.handleSecurityRequest("status", traceId);
        assertThat(result).contains("Vulnerabilities found");
    }

    @Test
    @DisplayName("handleSecurityRequest() returns patterns")
    void patterns() {
        String result = securityAgent.handleSecurityRequest("patterns", traceId);
        assertThat(result).contains("SQL Injection");
        assertThat(result).contains("CRITICAL");
    }

    @Test
    @DisplayName("id() returns stable id")
    void idStable() {
        assertThat(securityAgent.id()).isEqualTo(AgentId.of("00000000-0000-0000-0000-000000000009"));
    }

    @Test
    @DisplayName("capabilities() includes SECURITY_ANALYSIS")
    void capabilities() {
        assertThat(securityAgent.capabilities().supports(Intent.SECURITY_ANALYSIS)).isTrue();
    }

    @Test
    @DisplayName("health() returns HEALTHY with no requests")
    void healthEmpty() {
        assertThat(securityAgent.health()).isEqualTo(HealthStatus.HEALTHY);
    }
}
