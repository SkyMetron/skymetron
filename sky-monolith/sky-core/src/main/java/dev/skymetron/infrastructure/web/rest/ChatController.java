package dev.skymetron.infrastructure.web.rest;

import dev.skymetron.application.agent.CeoAgent;
import dev.skymetron.application.provider.ProviderRegistry;
import dev.skymetron.domain.execution.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Chat", description = "Conversational AI — chat, provider status, CEO metrics")
public class ChatController {

    private final CeoAgent ceoAgent;
    private final ProviderRegistry providerRegistry;

    public ChatController(CeoAgent ceoAgent, ProviderRegistry providerRegistry) {
        this.ceoAgent = ceoAgent;
        this.providerRegistry = providerRegistry;
    }

    @Operation(summary = "Send a chat message", description = "Routes message through CEO agent for intent classification and processing")
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        Intent intent = ceoAgent.classifyIntent(request.message());
        AgentRequest agentRequest = AgentRequest.simple(
                ceoAgent.id(),
                intent,
                request.message());

        AgentResponse response = ceoAgent.process(agentRequest).join();

        if (!response.success()) {
            return ResponseEntity.internalServerError().body(new ChatResponse(
                    response.content(), null, false, null, null, null,
                    response.duration().toMillis(), 0, 0));
        }

        Intent classified = ceoAgent.classifyIntent(request.message());
        String routedAgent = switch (classified) {
            case MEMORY_QUERY, MEMORY_STORE -> "MemoryAgent";
            case TOOL_EXECUTION -> "ToolAgent";
            default -> "CeoAgent (LLM)";
        };
        String taskType = ceoAgent.selectTaskType(classified, request.message()).name();

        return ResponseEntity.ok(new ChatResponse(
                response.content(),
                extractProvider(response),
                false,
                classified.name(),
                taskType,
                routedAgent,
                response.duration().toMillis(),
                0,
                0
        ));
    }

    @Operation(summary = "List LLM providers", description = "Returns availability, free tier status, and last error per provider")
    @GetMapping("/providers/status")
    public ResponseEntity<List<ProviderStatusResponse>> providerStatus() {
        List<ProviderStatusResponse> status = providerRegistry.getStatus().stream()
                .map(s -> new ProviderStatusResponse(s.providerId(), s.available(), s.free(), s.lastError()))
                .toList();
        return ResponseEntity.ok(status);
    }

    @Operation(summary = "CEO agent metrics", description = "Request counts, success rate, health, and status of the CEO agent")
    @GetMapping("/agents/ceo/metrics")
    public ResponseEntity<Map<String, Object>> ceoMetrics() {
        AgentMetrics m = ceoAgent.metrics();
        return ResponseEntity.ok(Map.of(
                "totalRequests", m.totalRequests(),
                "successful", m.successfulRequests(),
                "failed", m.failedRequests(),
                "averageLatencyMs", m.averageLatencyMs(),
                "successRate", m.successRate(),
                "health", ceoAgent.health().name(),
                "status", ceoAgent.status().name()
        ));
    }

    private String extractProvider(AgentResponse response) {
        return "ceo-routed";
    }
}
