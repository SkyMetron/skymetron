package dev.skymetron.infrastructure.web.rest;

import dev.skymetron.domain.execution.Agent;
import dev.skymetron.domain.execution.AgentId;
import dev.skymetron.infrastructure.metrics.SkyMetricsRegistry;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminController tests")
class AdminControllerTest {

    @Mock
    SkyMetricsRegistry metrics;

    @Mock
    Agent mockAgent;

    AdminController controller;

    @BeforeEach
    void setUp() {
        when(mockAgent.id()).thenReturn(AgentId.of("00000000-0000-0000-0000-000000000001"));
        controller = new AdminController(List.of(mockAgent), metrics);
    }

    @Test
    @DisplayName("GET /api/admin/memory returns heap and non-heap info")
    void memory() {
        ResponseEntity<Map<String, Object>> response = controller.memory();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("heap");
        assertThat(response.getBody()).containsKey("nonHeap");
    }

    @Test
    @DisplayName("GET /api/admin/threads returns thread info")
    void threads() {
        ResponseEntity<Map<String, Object>> response = controller.threads();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("active");
        assertThat(response.getBody()).containsKey("daemon");
        assertThat(response.getBody()).containsKey("peak");
    }

    @Test
    @DisplayName("GET /api/admin/benchmark returns agent latencies")
    void benchmark() {
        ResponseEntity<Map<String, Object>> response = controller.benchmark();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("agents");
        assertThat(response.getBody()).containsKey("p95Ms");
        assertThat(response.getBody()).containsKey("status");
    }

    @Test
    @DisplayName("benchmark p95 is 0 when no agent data")
    void benchmarkP95Empty() {
        AdminController emptyController = new AdminController(List.of(), metrics);
        ResponseEntity<Map<String, Object>> response = emptyController.benchmark();

        assertThat(response.getBody().get("p95Ms")).isEqualTo(0.0);
    }
}
