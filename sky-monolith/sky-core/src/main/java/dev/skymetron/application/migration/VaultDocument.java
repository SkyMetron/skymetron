package dev.skymetron.application.migration;

import dev.skymetron.domain.memory.MemoryType;

import java.nio.file.Path;
import java.util.List;

/**
 * Result of scanning a single ENIAC vault document.
 *
 * @param path         original file path
 * @param content      cleaned markdown content (frontmatter stripped)
 * @param type         classified memory type
 * @param title        extracted title (filename or frontmatter)
 * @param sizeBytes    file size
 * @param sha256       content hash for duplicate detection
 * @param tags         Obsidian tags extracted from content
 */
public record VaultDocument(
        Path path,
        String content,
        MemoryType type,
        String title,
        long sizeBytes,
        String sha256,
        List<String> tags
) {
}
