package dev.skymetron.infrastructure.web.rest.bootstrap;

import dev.skymetron.application.privacy.PrivacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/privacy")
@Tag(name = "Privacy", description = "Privacy management — LGPD compliance and data control")
public class PrivacyController {

    private final PrivacyService privacyService;

    public PrivacyController(PrivacyService privacyService) {
        this.privacyService = privacyService;
    }

    @Operation(summary = "Export all data", description = "Exports all user data to a temporary directory")
    @PostMapping("/export")
    public ResponseEntity<?> exportData() {
        try {
            var result = privacyService.exportData();
            return ResponseEntity.ok(Map.of(
                "path", result.path(),
                "sizeBytes", result.sizeBytes()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Export failed",
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Delete local account", description = "Removes all local data and configuration")
    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount() {
        try {
            privacyService.deleteLocalAccount();
            return ResponseEntity.ok(Map.of("status", "deleted"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Account deletion failed",
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Clear cache", description = "Clears temporary data and logs")
    @PostMapping("/clear-cache")
    public ResponseEntity<?> clearCache() {
        try {
            privacyService.clearCache();
            return ResponseEntity.ok(Map.of("status", "cleared"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Cache clear failed",
                "message", e.getMessage()
            ));
        }
    }
}
