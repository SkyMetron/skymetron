package dev.skymetron.infrastructure.web.rest;

import dev.skymetron.application.migration.EniacVaultMigration;
import dev.skymetron.application.migration.MigrationReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/migration")
@Tag(name = "Migration", description = "Vault migration from legacy formats")
public class MigrationController {

    private final EniacVaultMigration migration;

    public MigrationController(EniacVaultMigration migration) {
        this.migration = migration;
    }

    @Operation(summary = "Run vault migration", description = "Migrate data from legacy Eniac vault format")
    @PostMapping("/run")
    public ResponseEntity<MigrationReport> run(
            @RequestParam(required = false) String vaultPath) {
        Path explicit = vaultPath != null && !vaultPath.isBlank() ? Path.of(vaultPath) : null;
        MigrationReport report = migration.run(explicit);
        return ResponseEntity.ok(report);
    }
}
