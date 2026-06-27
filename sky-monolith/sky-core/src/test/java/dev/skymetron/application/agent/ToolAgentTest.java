package dev.skymetron.application.agent;

import dev.skymetron.application.usecase.AgentSafetyPolicy;
import dev.skymetron.domain.execution.*;
import dev.skymetron.domain.tool.*;
import dev.skymetron.infrastructure.messaging.EventPublisher;
import dev.skymetron.infrastructure.metrics.SkyMetricsRegistry;
import dev.skymetron.infrastructure.tool.FilesystemToolAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ToolAgent tests")
class ToolAgentTest {

    private ToolAgent toolAgent;
    private FilesystemToolAdapter filesystem;

    @BeforeEach
    void setUp() {
        AgentSafetyPolicy safetyPolicy = new AgentSafetyPolicy();
        filesystem = new FilesystemToolAdapter(safetyPolicy);
        toolAgent = new ToolAgent(List.of(filesystem), Mockito.mock(EventPublisher.class), Mockito.mock(SkyMetricsRegistry.class));
    }

    @Test
    @DisplayName("parseToolRequest() extracts tool name and key=value params")
    void parseToolRequestKv() {
        Map<String, Object> params = toolAgent.parseToolRequest("filesystem|action=list|path=/tmp");

        assertThat(params.get("_tool")).isEqualTo("filesystem");
        assertThat(params.get("action")).isEqualTo("list");
        assertThat(params.get("path")).isEqualTo("/tmp");
    }

    @Test
    @DisplayName("parseToolRequest() handles simple positional format")
    void parseToolRequestPositional() {
        Map<String, Object> params = toolAgent.parseToolRequest("filesystem|list|/tmp");

        assertThat(params.get("_tool")).isEqualTo("filesystem");
        assertThat(params.get("action")).isEqualTo("list");
        assertThat(params.get("path")).isEqualTo("/tmp");
    }

    @Test
    @DisplayName("parseToolRequest() with empty payload defaults to filesystem")
    void parseToolRequestEmpty() {
        Map<String, Object> params = toolAgent.parseToolRequest("");

        assertThat(params.get("_tool")).isEqualTo("filesystem");
    }

    @Test
    @DisplayName("process() executes filesystem list successfully")
    void processListFiles() {
        Path tempDir = Path.of(java.lang.System.getProperty("java.io.tmpdir"));
        AgentRequest req = AgentRequest.simple(toolAgent.id(), Intent.TOOL_EXECUTION,
                "filesystem|action=list|path=" + tempDir.toString());

        AgentResponse resp = toolAgent.process(req).join();

        assertThat(resp.success()).isTrue();
    }

    @Test
    @DisplayName("process() executes filesystem read successfully")
    void processReadFile() throws Exception {
        Path tempFile = Path.of(java.lang.System.getProperty("java.io.tmpdir"), "skymetron-test-" + java.util.UUID.randomUUID() + ".txt");
        java.nio.file.Files.writeString(tempFile, "hello world");

        AgentRequest req = AgentRequest.simple(toolAgent.id(), Intent.TOOL_EXECUTION,
                "filesystem|action=read|path=" + tempFile.toString());
        AgentResponse resp = toolAgent.process(req).join();

        assertThat(resp.success()).isTrue();
        assertThat(resp.content()).contains("hello world");
        java.nio.file.Files.deleteIfExists(tempFile);
    }

    @Test
    @DisplayName("process() returns failure for unknown tool")
    void processUnknownTool() {
        AgentRequest req = AgentRequest.simple(toolAgent.id(), Intent.TOOL_EXECUTION,
                "nonexistent|action=test");
        AgentResponse resp = toolAgent.process(req).join();

        assertThat(resp.success()).isFalse();
        assertThat(resp.content()).contains("Unknown tool");
    }

    @Test
    @DisplayName("process() denies access to sensitive paths")
    void processDeniesSensitivePath() {
        AgentRequest req = AgentRequest.simple(toolAgent.id(), Intent.TOOL_EXECUTION,
                "filesystem|action=read|path=C:\\Users\\user\\.ssh\\id_rsa");
        AgentResponse resp = toolAgent.process(req).join();

        assertThat(resp.success()).isFalse();
        assertThat(resp.content()).contains("DENIED");
    }

    @Test
    @DisplayName("id() returns stable ToolAgent id")
    void idStable() {
        assertThat(toolAgent.id()).isEqualTo(AgentId.of("00000000-0000-0000-0000-000000000003"));
    }

    @Test
    @DisplayName("availableTools() lists registered tools")
    void availableTools() {
        List<String> tools = toolAgent.availableTools();
        assertThat(tools).contains("filesystem");
    }

    @Test
    @DisplayName("capabilities() declares TOOL_EXECUTION intent")
    void capabilitiesDeclaresIntent() {
        AgentCapabilities caps = toolAgent.capabilities();
        assertThat(caps.supports(Intent.TOOL_EXECUTION)).isTrue();
    }

    @Test
    @DisplayName("health() returns HEALTHY with no requests")
    void healthEmpty() {
        assertThat(toolAgent.health()).isEqualTo(HealthStatus.HEALTHY);
    }
}
