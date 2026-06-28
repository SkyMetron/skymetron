package dev.skymetron.infrastructure.web.rest.bootstrap;

import dev.skymetron.application.update.UpdateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/update")
@Tag(name = "Update", description = "Update checking service — GitHub Releases")
public class UpdateController {

    private final UpdateService updateService;

    public UpdateController(UpdateService updateService) {
        this.updateService = updateService;
    }

    @Operation(summary = "Check for updates", description = "Checks GitHub Releases for the latest version")
    @GetMapping("/check")
    public ResponseEntity<?> checkForUpdates() {
        var release = updateService.checkLatestRelease();
        if (release.isPresent()) {
            var r = release.get();
            return ResponseEntity.ok(Map.of(
                "available", true,
                "tagName", r.tagName(),
                "name", r.name(),
                "publishedAt", r.publishedAt(),
                "prerelease", r.prerelease()
            ));
        }
        return ResponseEntity.ok(Map.of("available", false));
    }
}
