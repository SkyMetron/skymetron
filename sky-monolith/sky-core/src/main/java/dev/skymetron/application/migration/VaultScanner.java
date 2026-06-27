package dev.skymetron.application.migration;

import dev.skymetron.domain.memory.MemoryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Phase 1-3: Scans the sky-vault directory, classifies documents, strips
 * frontmatter, extracts tags, and computes content hashes.
 *
 * <p>Vault source: <a href="https://github.com/SkyMetron/sky-vault">SkyMetron/sky-vault</a>
 * (extracted from legacy ENIAC_METRON).
 */
@Component
public class VaultScanner {

    private static final Logger log = LoggerFactory.getLogger(VaultScanner.class);
    private static final Pattern FRONTMATTER = Pattern.compile("(?s)^---\\n.*?\\n---\\n");
    private static final Pattern TAG_PATTERN = Pattern.compile("(?:^|\\s)#([a-zA-Z0-9_/-]+)");
    private static final Pattern TITLE_PATTERN = Pattern.compile("(?m)^#\\s+(.+)$");

    /**
     * Recursively scan a directory for markdown files.
     *
     * @param vaultRoot root of the vault (e.g. ../sky-vault/knowledge)
     * @return list of classified vault documents
     */
    public List<VaultDocument> scan(Path vaultRoot) throws IOException {
        List<VaultDocument> docs = new ArrayList<>();
        if (!Files.isDirectory(vaultRoot)) {
            log.warn("Vault root does not exist or is not a directory: {}", vaultRoot);
            return docs;
        }

        try (var paths = Files.walk(vaultRoot)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".md"))
                 .filter(p -> !isInExcludedDir(p, vaultRoot))
                 .forEach(p -> {
                     try {
                         docs.add(toDocument(p, vaultRoot));
                     } catch (IOException e) {
                         log.warn("Failed to read {}: {}", p, e.getMessage());
                     }
                 });
        }
        log.info("Scanned {} markdown documents from {}", docs.size(), vaultRoot);
        return docs;
    }

    private VaultDocument toDocument(Path file, Path vaultRoot) throws IOException {
        String raw = Files.readString(file);
        long size = Files.size(file);
        String sha = sha256(raw);

        String content = stripFrontmatter(raw);
        String title = extractTitle(content, file);
        List<String> tags = extractTags(raw);
        MemoryType type = classify(file, vaultRoot, content);

        return new VaultDocument(file, content, type, title, size, sha, tags);
    }

    private boolean isInExcludedDir(Path path, Path vaultRoot) {
        Path relative = vaultRoot.relativize(path);
        for (int i = 0; i < relative.getNameCount(); i++) {
            String part = relative.getName(i).toString();
            if (part.equals("99-ARCHIVE") || part.equals(".obsidian") || part.equals(".agents") || part.equals(".opencode")) {
                return true;
            }
        }
        return false;
    }

    private String stripFrontmatter(String raw) {
        Matcher m = FRONTMATTER.matcher(raw);
        if (m.find()) {
            return raw.substring(m.end()).strip();
        }
        return raw.strip();
    }

    private String extractTitle(String content, Path file) {
        Matcher m = TITLE_PATTERN.matcher(content);
        if (m.find()) {
            return m.group(1).strip();
        }
        String name = file.getFileName().toString();
        return name.substring(0, name.length() - 3);
    }

    private List<String> extractTags(String raw) {
        List<String> tags = new ArrayList<>();
        Matcher m = TAG_PATTERN.matcher(raw);
        while (m.find()) {
            tags.add(m.group(1));
        }
        return tags;
    }

    MemoryType classify(Path file, Path vaultRoot, String content) {
        Path relative = vaultRoot.relativize(file);
        String firstDir = relative.getNameCount() > 0 ? relative.getName(0).toString() : "";
        String lower = content.toLowerCase();

        if (firstDir.contains("Sessoes") || firstDir.contains("Session") || relative.toString().contains("Resumo")) {
            return MemoryType.SESSION_CONTEXT;
        }
        if (firstDir.contains("Regras") || firstDir.contains("Skill") || lower.contains("skill")) {
            return MemoryType.SKILL_EMBEDDINGS;
        }
        if (lower.contains("prefer") || lower.contains("gosto") || lower.contains("meu nome") || lower.contains("sobre mim")) {
            return MemoryType.USER_FACTS;
        }
        return MemoryType.PROJECT_KNOWLEDGE;
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
