package dev.skymetron.application.migration;

import dev.skymetron.application.port.out.EmbeddingPort;
import dev.skymetron.application.usecase.MemoryService;
import dev.skymetron.domain.memory.MemoryEntry;
import dev.skymetron.domain.memory.MemoryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orchestrates the ENIAC Vault → SkyMetron migration.
 *
 * <p>Phases:
 * <ol>
 *   <li>Inventário — scan vault directories for markdown files</li>
 *   <li>Validação — skip exact duplicates by SHA-256 hash</li>
 *   <li>Transformação — classify type, extract metadata</li>
 *   <li>Importação — embed each document and persist to memory_entries</li>
 *   <li>Deduplicação — similarity > 0.95 detected on save (merged_into)</li>
 * </ol>
 *
 * <p>Triggered via {@code POST /api/migration/run} (see MigrationController).
 */
@Service
public class EniacVaultMigration {

    private static final Logger log = LoggerFactory.getLogger(EniacVaultMigration.class);
    private static final int BATCH_SIZE = 50;

    private final VaultScanner scanner;
    private final EmbeddingPort embeddingPort;
    private final MemoryService memoryService;

    @Value("${sky.migration.eniac-vault-path:../ENIAC_METRON}")
    private String vaultPath;

    public EniacVaultMigration(VaultScanner scanner, EmbeddingPort embeddingPort, MemoryService memoryService) {
        this.scanner = scanner;
        this.embeddingPort = embeddingPort;
        this.memoryService = memoryService;
    }

    public MigrationReport run(Path explicitVaultPath) {
        Path vaultRoot = explicitVaultPath != null ? explicitVaultPath : Path.of(vaultPath);
        long start = System.currentTimeMillis();
        log.info("Starting ENIAC Vault migration from {}", vaultRoot);

        List<String> errors = new ArrayList<>();
        List<String> lowConfidence = new ArrayList<>();
        Map<MemoryType, Integer> byType = new HashMap<>();
        int imported = 0;
        int skippedHashDupes = 0;
        int mergedSemanticDupes = 0;

        try {
            List<VaultDocument> docs = scanner.scan(vaultRoot);
            Set<String> seenHashes = new HashSet<>();

            int batch = 0;
            for (VaultDocument doc : docs) {
                try {
                    if (seenHashes.contains(doc.sha256())) {
                        skippedHashDupes++;
                        continue;
                    }
                    seenHashes.add(doc.sha256());

                    float[] embedding = embeddingPort.embed(doc.content());
                    double confidence = doc.type() == MemoryType.PROJECT_KNOWLEDGE ? 0.8 : 0.9;

                    MemoryEntry entry = memoryService.saveWithEmbedding(
                            doc.content(), embedding, doc.type(), "eniac-vault-migration", confidence);

                    entry.addMetadata("original_title", doc.title());
                    entry.addMetadata("original_path", doc.path().toString());
                    if (!doc.tags().isEmpty()) {
                        entry.addMetadata("tags", doc.tags());
                    }

                    byType.merge(doc.type(), 1, Integer::sum);
                    if (entry.getMergedInto() != null) {
                        mergedSemanticDupes++;
                    } else {
                        imported++;
                    }
                    if (confidence < 0.85) {
                        lowConfidence.add(entry.getId() + " | " + doc.title());
                    }

                    batch++;
                    if (batch % BATCH_SIZE == 0) {
                        log.info("Migration progress: {}/{} documents processed", batch, docs.size());
                    }
                } catch (Exception e) {
                    log.warn("Failed to migrate {}: {}", doc.path(), e.getMessage());
                    errors.add(doc.path() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Migration failed: {}", e.getMessage(), e);
            errors.add("FATAL: " + e.getMessage());
        }

        long duration = System.currentTimeMillis() - start;
        MigrationReport report = new MigrationReport(
                vaultRoot.toString(),
                byType.values().stream().mapToInt(Integer::intValue).sum() + skippedHashDupes + errors.size(),
                imported,
                skippedHashDupes,
                mergedSemanticDupes,
                errors.size(),
                duration,
                byType,
                lowConfidence,
                errors);

        log.info("Migration complete: imported={}, skipped={}, merged={}, errors={}, duration={}ms",
                imported, skippedHashDupes, mergedSemanticDupes, errors.size(), duration);
        return report;
    }
}
