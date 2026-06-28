package dev.skymetron.application.privacy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class PrivacyService {

    private static final Logger log = LoggerFactory.getLogger(PrivacyService.class);
    private static final Path HOME = Paths.get(System.getProperty("user.home"));
    private static final Path WORKSPACE_ROOT = HOME.resolve("SkyMetron");
    private static final Path CONFIG_DIR = HOME.resolve(".skymetron");

    public ExportResult exportData() throws IOException {
        Path exportDir = Files.createTempDirectory("skymetron-export-");
        Path configFile = CONFIG_DIR.resolve("config.json");
        Path envFile = WORKSPACE_ROOT.resolve(".env");
        Path vaultDir = WORKSPACE_ROOT.resolve("vault");

        if (Files.exists(configFile)) {
            Files.copy(configFile, exportDir.resolve("config.json"));
        }
        if (Files.exists(envFile)) {
            Files.copy(envFile, exportDir.resolve(".env"));
        }
        if (Files.exists(vaultDir)) {
            copyDirectory(vaultDir, exportDir.resolve("vault"));
        }

        long size;
        try (Stream<Path> walk = Files.walk(exportDir)) {
            size = walk.filter(Files::isRegularFile)
                .mapToLong(p -> {
                    try { return Files.size(p); }
                    catch (IOException e) { return 0; }
                }).sum();
        }

        log.info("Data exported to {} ({} bytes)", exportDir, size);
        return new ExportResult(exportDir.toString(), size);
    }

    public void deleteLocalAccount() throws IOException {
        if (Files.exists(WORKSPACE_ROOT)) {
            try (Stream<Path> walk = Files.walk(WORKSPACE_ROOT)) {
                walk.sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); }
                        catch (IOException e) { log.warn("Could not delete {}", p); }
                    });
            }
        }
        if (Files.exists(CONFIG_DIR)) {
            try (Stream<Path> walk = Files.walk(CONFIG_DIR)) {
                walk.sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); }
                        catch (IOException e) { log.warn("Could not delete {}", p); }
                    });
            }
        }
        log.info("Local account deleted");
    }

    public void clearCache() throws IOException {
        Path cacheDir = WORKSPACE_ROOT.resolve("cache");
        Path logsDir = WORKSPACE_ROOT.resolve("logs");
        if (Files.exists(cacheDir)) {
            try (Stream<Path> walk = Files.walk(cacheDir)) {
                walk.sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); }
                        catch (IOException e) { log.warn("Could not delete cache {}", p); }
                    });
            }
        }
        if (Files.exists(logsDir)) {
            try (Stream<Path> walk = Files.walk(logsDir)) {
                walk.sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); }
                        catch (IOException e) { log.warn("Could not delete log {}", p); }
                    });
            }
        }
        log.info("Cache cleared");
    }

    public boolean hasAcceptedTerms() {
        Path marker = CONFIG_DIR.resolve(".terms-accepted");
        return Files.exists(marker);
    }

    public boolean hasAcceptedLgpd() {
        Path marker = CONFIG_DIR.resolve(".lgpd-accepted");
        return Files.exists(marker);
    }

    public void acceptTerms() throws IOException {
        Files.createDirectories(CONFIG_DIR);
        Files.createFile(CONFIG_DIR.resolve(".terms-accepted"));
        log.info("Terms accepted");
    }

    public void acceptLgpd() throws IOException {
        Files.createDirectories(CONFIG_DIR);
        Files.createFile(CONFIG_DIR.resolve(".lgpd-accepted"));
        log.info("LGPD accepted");
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        try (Stream<Path> walk = Files.walk(source)) {
            walk.forEach(sourcePath -> {
                try {
                    Path targetPath = target.resolve(source.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.copy(sourcePath, targetPath);
                    }
                } catch (IOException e) {
                    log.warn("Could not copy {}", sourcePath);
                }
            });
        }
    }

    public record ExportResult(String path, long sizeBytes) {}
}
