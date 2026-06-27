package dev.skymetron.infrastructure.web.rest;

import dev.skymetron.application.scheduler.LoopScheduler;
import dev.skymetron.application.scheduler.LoopState;
import dev.skymetron.application.scheduler.UserActivityTracker;
import dev.skymetron.domain.observation.OperationMode;
import dev.skymetron.infrastructure.audit.AuditLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loops")
@Tag(name = "Loops", description = "Autonomous loop scheduler — status, pause, resume, mode control")
public class LoopController {

    private final LoopScheduler loopScheduler;
    private final UserActivityTracker activityTracker;

    public LoopController(LoopScheduler loopScheduler, UserActivityTracker activityTracker) {
        this.loopScheduler = loopScheduler;
        this.activityTracker = activityTracker;
    }

    @Operation(summary = "All loop statuses", description = "Current mode, idle time, and status of all registered loops")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        List<LoopState> states = loopScheduler.getStatus();
        OperationMode mode = activityTracker.getCurrentMode();
        return ResponseEntity.ok(Map.of(
                "mode", mode.name(),
                "idleTime", activityTracker.getIdleTime().toMinutes() + "m",
                "loops", states
        ));
    }

    @Operation(summary = "Single loop status")
    @GetMapping("/status/{name}")
    public ResponseEntity<?> statusByName(@PathVariable String name) {
        LoopState state = loopScheduler.getStatus(name);
        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
                "mode", activityTracker.getCurrentMode().name(),
                "loop", state
        ));
    }

    @Operation(summary = "Pause a loop")
    @AuditLog(action = "loop.pause", resource = "#name")
    @PostMapping("/{name}/pause")
    public ResponseEntity<Map<String, String>> pause(@PathVariable String name) {
        var state = loopScheduler.getStatus(name);
        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        loopScheduler.pause(name);
        return ResponseEntity.ok(Map.of("status", "paused", "loop", name));
    }

    @Operation(summary = "Resume a paused loop")
    @AuditLog(action = "loop.resume", resource = "#name")
    @PostMapping("/{name}/resume")
    public ResponseEntity<Map<String, String>> resume(@PathVariable String name) {
        var state = loopScheduler.getStatus(name);
        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        loopScheduler.resume(name);
        return ResponseEntity.ok(Map.of("status", "resumed", "loop", name));
    }

    @Operation(summary = "Set operation mode", description = "Override mode: ACTIVE, IDLE, DEEP, or SLEEP")
    @AuditLog(action = "loop.setMode", resource = "#body.mode()")
    @PostMapping("/mode")
    public ResponseEntity<Map<String, String>> setMode(@Valid @RequestBody ModeRequest body) {
        try {
            OperationMode mode = OperationMode.valueOf(body.mode().toUpperCase());
            activityTracker.setMode(mode);
            return ResponseEntity.ok(Map.of("mode", mode.name(), "status", "updated"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid mode: " + body.mode()));
        }
    }

    @Operation(summary = "Current operation mode")
    @GetMapping("/mode")
    public ResponseEntity<Map<String, Object>> getMode() {
        OperationMode mode = activityTracker.getCurrentMode();
        return ResponseEntity.ok(Map.of(
                "mode", mode.name(),
                "idleTime", activityTracker.getIdleTime().toMinutes() + "m"
        ));
    }
}
