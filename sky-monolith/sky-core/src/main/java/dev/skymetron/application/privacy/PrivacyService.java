package dev.skymetron.application.privacy;

import dev.skymetron.application.bootstrap.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
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
    public static final String TERMS_VERSION = "2026-06-28-v1";
    public static final String PRIVACY_VERSION = "2026-06-28-v1";
    public static final String APP_VERSION = "0.2.1-beta";

    private final ConfigService configService;

    public PrivacyService() {
        this(new ConfigService());
    }

    @Autowired
    public PrivacyService(ConfigService configService) {
        this.configService = configService;
    }

    public ExportResult exportData() throws IOException {
        Path exportDir = Files.createTempDirectory("skymetron-export-");
        Path configFile = CONFIG_DIR.resolve("config.json");
        Path envFile = WORKSPACE_ROOT.resolve(".env");
        Path vaultDir = WORKSPACE_ROOT.resolve("vault");

        if (Files.exists(configFile)) {
            copyMaskedFile(configFile, exportDir.resolve("config.json"));
        }
        if (Files.exists(envFile)) {
            copyMaskedFile(envFile, exportDir.resolve(".env"));
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
        log.info("Cache and logs cleared");
    }

    public boolean hasAcceptedTerms() {
        Map<String, Object> status = legalStatus();
        return Boolean.TRUE.equals(status.get("termsAccepted"));
    }

    public boolean hasAcceptedLgpd() {
        Map<String, Object> status = legalStatus();
        return Boolean.TRUE.equals(status.get("lgpdAccepted"));
    }

    public void acceptTerms() throws IOException {
        acceptTerms(null);
    }

    public void acceptTerms(String username) throws IOException {
        saveAcceptance(true, hasAcceptedLgpd(), username);
        log.info("Terms accepted version={}", TERMS_VERSION);
    }

    public void acceptLgpd() throws IOException {
        acceptLgpd(null);
    }

    public void acceptLgpd(String username) throws IOException {
        saveAcceptance(hasAcceptedTerms(), true, username);
        log.info("LGPD accepted version={}", PRIVACY_VERSION);
    }

    public Map<String, Object> legalStatus() {
        Map<String, Object> config;
        try {
            config = configService.loadConfig();
        } catch (IOException e) {
            config = Map.of();
        }

        boolean termsAccepted = Boolean.TRUE.equals(config.get("termsAccepted"))
            && TERMS_VERSION.equals(config.get("termsVersion"));
        boolean lgpdAccepted = Boolean.TRUE.equals(config.get("lgpdAccepted"))
            && PRIVACY_VERSION.equals(config.get("privacyVersion"));

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("termsAccepted", termsAccepted);
        status.put("lgpdAccepted", lgpdAccepted);
        status.put("termsVersion", config.getOrDefault("termsVersion", TERMS_VERSION));
        status.put("privacyVersion", config.getOrDefault("privacyVersion", PRIVACY_VERSION));
        status.put("acceptedAt", config.get("acceptedAt"));
        status.put("acceptedByGitHubUser", config.get("acceptedByGitHubUser"));
        status.put("appVersion", config.getOrDefault("appVersion", APP_VERSION));
        return status;
    }

    public String maskSecrets(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return text
            .replaceAll("github_pat_[A-Za-z0-9_]{6,}", "github_pat_***")
            .replaceAll("ghp_[A-Za-z0-9_]{6,}", "ghp_***")
            .replaceAll("nvapi-[A-Za-z0-9_-]{6,}", "nvapi-***")
            .replaceAll("gsk_[A-Za-z0-9_]{6,}", "gsk_***")
            .replaceAll("AIza[A-Za-z0-9_-]{6,}", "AIza***")
            .replaceAll("sk-or-[A-Za-z0-9_-]{6,}", "sk-or-***")
            .replaceAll("sk-[A-Za-z0-9_-]{6,}", "sk-***");
    }

    private void saveAcceptance(boolean termsAccepted, boolean lgpdAccepted, String username) throws IOException {
        configService.saveLegalAcceptance(
            termsAccepted,
            lgpdAccepted,
            TERMS_VERSION,
            PRIVACY_VERSION,
            Instant.now().toString(),
            username,
            APP_VERSION
        );
    }

    private void copyMaskedFile(Path source, Path target) throws IOException {
        Files.writeString(target, maskSecrets(Files.readString(source)));
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
