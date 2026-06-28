package dev.skymetron.infrastructure.web.rest.bootstrap;

import dev.skymetron.application.bootstrap.ConfigService;
import dev.skymetron.application.bootstrap.VaultBootstrapService;
import dev.skymetron.application.bootstrap.WorkspaceBootstrapService;
import dev.skymetron.application.bootstrap.WorkspaceConfig;
import dev.skymetron.application.privacy.PrivacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bootstrap")
@Tag(name = "Bootstrap", description = "Workspace and vault bootstrap after authentication")
public class BootstrapController {

    private final WorkspaceBootstrapService workspaceBootstrapService;
    private final VaultBootstrapService vaultBootstrapService;
    private final ConfigService configService;
    private final PrivacyService privacyService;

    public BootstrapController(WorkspaceBootstrapService workspaceBootstrapService,
                               VaultBootstrapService vaultBootstrapService,
                               ConfigService configService,
                               PrivacyService privacyService) {
        this.workspaceBootstrapService = workspaceBootstrapService;
        this.vaultBootstrapService = vaultBootstrapService;
        this.configService = configService;
        this.privacyService = privacyService;
    }

    @Operation(summary = "Check workspace status", description = "Returns current workspace config and detected tools")
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@RequestAttribute String username) {
        boolean isMaintainer = "Joao-Aschenbrenner".equals(username);
        WorkspaceConfig config = workspaceBootstrapService.buildConfig(username, isMaintainer);
        var map = new HashMap<String, Object>();
        map.put("workspaceType", config.workspaceType());
        map.put("workspacePath", config.workspacePath().toString());
        map.put("vaultPath", config.vaultPath().toString());
        map.put("envFileExists", config.envFileExists());
        map.put("dockerDetected", config.dockerDetected());
        map.put("javaDetected", config.javaDetected());
        map.put("gitDetected", config.gitDetected());
        map.put("ollamaDetected", config.ollamaDetected());
        map.put("postgresDetected", config.postgresDetected());
        map.put("rabbitMqDetected", config.rabbitMqDetected());
        map.put("redisDetected", config.redisDetected());
        map.put("maintainer", isMaintainer);
        map.put("isComplete", config.isComplete());
        return ResponseEntity.ok(map);
    }

    @Operation(summary = "Create workspace", description = "Creates the workspace directory structure")
    @PostMapping("/workspace")
    public ResponseEntity<?> createWorkspace(@RequestAttribute String username) {
        try {
            boolean isMaintainer = "Joao-Aschenbrenner".equals(username);
            workspaceBootstrapService.ensureWorkspace(username, isMaintainer);
            WorkspaceConfig config = workspaceBootstrapService.buildConfig(username, isMaintainer);
            return ResponseEntity.ok(Map.of(
                "status", "created",
                "workspaceType", config.workspaceType(),
                "workspacePath", config.workspacePath().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to create workspace",
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Scan vault", description = "Scans the vault directory for markdown files")
    @PostMapping("/vault/scan")
    public ResponseEntity<?> scanVault(@RequestAttribute String username) {
        try {
            boolean isMaintainer = "Joao-Aschenbrenner".equals(username);
            WorkspaceConfig config = workspaceBootstrapService.buildConfig(username, isMaintainer);
            var result = vaultBootstrapService.scan(config.vaultPath());
            return ResponseEntity.ok(Map.of(
                "markdownFiles", result.markdownFiles(),
                "totalFiles", result.totalFiles(),
                "totalSizeBytes", result.totalSizeBytes(),
                "success", result.success()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to scan vault",
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Accept terms", description = "Mark terms of service as accepted")
    @PostMapping("/accept-terms")
    public ResponseEntity<?> acceptTerms() {
        try {
            privacyService.acceptTerms();
            return ResponseEntity.ok(Map.of("accepted", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to accept terms",
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Accept LGPD", description = "Mark LGPD privacy policy as accepted")
    @PostMapping("/accept-lgpd")
    public ResponseEntity<?> acceptLgpd() {
        try {
            privacyService.acceptLgpd();
            return ResponseEntity.ok(Map.of("accepted", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to accept LGPD",
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Check legal acceptance", description = "Check if terms and LGPD have been accepted")
    @GetMapping("/legal-status")
    public ResponseEntity<?> legalStatus() {
        return ResponseEntity.ok(Map.of(
            "termsAccepted", privacyService.hasAcceptedTerms(),
            "lgpdAccepted", privacyService.hasAcceptedLgpd()
        ));
    }
}
