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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("QaAgent tests")
class QaAgentTest {

    @Mock
    EventPublisher eventPublisher;
    @Mock
    SkyMetricsRegistry metrics;

    QaAgent qaAgent;
    UUID traceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        qaAgent = new QaAgent(eventPublisher, metrics);
    }

    @Test
    @DisplayName("handleTestRequest() runs a passing test")
    void runPassingTest() {
        String testCode = "assert result == true\nassert count == 1";
        String result = qaAgent.handleTestRequest("run|test1|MyClass|" + testCode, traceId);
        assertThat(result).contains("passed");
    }

    @Test
    @DisplayName("handleTestRequest() runs a failing test")
    void runFailingTest() {
        String testCode = "assert result == fail";
        String result = qaAgent.handleTestRequest("run|test2|MyClass|" + testCode, traceId);
        assertThat(result).contains("failed");
    }

    @Test
    @DisplayName("handleTestRequest() returns stats")
    void stats() {
        qaAgent.handleTestRequest("run|t1|MyClass|assert ok == true", traceId);
        String result = qaAgent.handleTestRequest("stats", traceId);
        assertThat(result).contains("Tests run:");
    }

    @Test
    @DisplayName("handleTestRequest() returns test history")
    void history() {
        qaAgent.handleTestRequest("run|myTest|Target|assert ok == true", traceId);
        String result = qaAgent.handleTestRequest("history|myTest", traceId);
        assertThat(result).contains("PASS");
    }

    @Test
    @DisplayName("handleTestRequest() lists tests")
    void listTests() {
        qaAgent.handleTestRequest("run|t1|Target|assert ok == true", traceId);
        String result = qaAgent.handleTestRequest("list", traceId);
        assertThat(result).contains("t1");
    }

    @Test
    @DisplayName("id() returns stable id")
    void idStable() {
        assertThat(qaAgent.id()).isEqualTo(AgentId.of("00000000-0000-0000-0000-000000000008"));
    }

    @Test
    @DisplayName("capabilities() includes QA_TEST")
    void capabilities() {
        assertThat(qaAgent.capabilities().supports(Intent.QA_TEST)).isTrue();
    }

    @Test
    @DisplayName("health() returns HEALTHY with no requests")
    void healthEmpty() {
        assertThat(qaAgent.health()).isEqualTo(HealthStatus.HEALTHY);
    }
}
