package dev.skymetron.infrastructure.web.rest;

import dev.skymetron.application.agent.TraceAgent;
import dev.skymetron.infrastructure.persistence.jpa.TraceEntry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/trace")
@Tag(name = "Trace", description = "Brain Trace — event timeline and observability")
public class TraceController {

    private final TraceAgent traceAgent;

    public TraceController(TraceAgent traceAgent) {
        this.traceAgent = traceAgent;
    }

    @Operation(summary = "Get event timeline", description = "Paginated list of all trace events, newest first")
    @GetMapping("/timeline")
    public ResponseEntity<Page<TraceEntry>> timeline(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ResponseEntity.ok(traceAgent.getTimeline(page, size));
    }

    @Operation(summary = "Get trace by agent ID", description = "Filter timeline by agent identifier")
    @GetMapping("/agent/{agentId}")
    public ResponseEntity<Page<TraceEntry>> agentTrace(
            @PathVariable String agentId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ResponseEntity.ok(traceAgent.getAgentTrace(agentId, page, size));
    }

    @Operation(summary = "Get trace by event type", description = "Filter timeline by event type (e.g. agent.invoked, memory.stored)")
    @GetMapping("/type/{eventType}")
    public ResponseEntity<Page<TraceEntry>> byType(
            @PathVariable String eventType,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ResponseEntity.ok(traceAgent.getByEventType(eventType, page, size));
    }

    @Operation(summary = "Count events by type", description = "Aggregated counts per event type (cached 1 minute)")
    @GetMapping("/counts")
    public ResponseEntity<Map<String, Long>> counts() {
        return ResponseEntity.ok(traceAgent.getCounts());
    }
}
