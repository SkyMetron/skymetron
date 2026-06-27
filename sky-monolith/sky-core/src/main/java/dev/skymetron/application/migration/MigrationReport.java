package dev.skymetron.application.migration;

import java.util.List;
import java.util.Map;

/**
 * Final migration report — serialized to JSON.
 */
public record MigrationReport(
        String vaultRoot,
        int totalScanned,
        int imported,
        int skippedDuplicates,
        int mergedDuplicates,
        int errors,
        long durationMs,
        Map<dev.skymetron.domain.memory.MemoryType, Integer> byType,
        List<String> lowConfidenceEntries,
        List<String> errorMessages
) {
}
