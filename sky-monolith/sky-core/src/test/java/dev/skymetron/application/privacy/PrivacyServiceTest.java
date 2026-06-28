package dev.skymetron.application.privacy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PrivacyService tests")
class PrivacyServiceTest {

    private PrivacyService service;
    private Path configDir;
    private Path workspaceRoot;

    @BeforeEach
    void setUp() {
        service = new PrivacyService();
        configDir = Path.of(System.getProperty("user.home"), ".skymetron");
        workspaceRoot = Path.of(System.getProperty("user.home"), "SkyMetron");
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(configDir.resolve(".terms-accepted"));
        Files.deleteIfExists(configDir.resolve(".lgpd-accepted"));
        deleteIfExists(workspaceRoot.resolve("cache"));
        deleteIfExists(workspaceRoot.resolve("logs"));
    }

    @Test
    @DisplayName("hasAcceptedTerms returns false initially")
    void termsNotAccepted() {
        assertThat(service.hasAcceptedTerms()).isFalse();
    }

    @Test
    @DisplayName("acceptTerms creates marker file")
    void acceptTerms() throws IOException {
        service.acceptTerms();
        assertThat(service.hasAcceptedTerms()).isTrue();
    }

    @Test
    @DisplayName("hasAcceptedLgpd returns false initially")
    void lgpdNotAccepted() {
        assertThat(service.hasAcceptedLgpd()).isFalse();
    }

    @Test
    @DisplayName("acceptLgpd creates marker file")
    void acceptLgpd() throws IOException {
        service.acceptLgpd();
        assertThat(service.hasAcceptedLgpd()).isTrue();
    }

    @Test
    @DisplayName("clearCache removes cache and logs directories")
    void clearCache() throws IOException {
        Path cacheDir = workspaceRoot.resolve("cache");
        Path logsDir = workspaceRoot.resolve("logs");
        Files.createDirectories(cacheDir);
        Files.createDirectories(logsDir);
        Files.writeString(cacheDir.resolve("temp.dat"), "data");
        Files.writeString(logsDir.resolve("app.log"), "log");

        service.clearCache();

        assertThat(cacheDir).doesNotExist();
        assertThat(logsDir).doesNotExist();
    }

    @Test
    @DisplayName("clearCache does not fail when cache/logs do not exist")
    void clearCacheNonExistent() throws IOException {
        service.clearCache();
    }

    @Test
    @DisplayName("exportData succeeds")
    void exportData() throws IOException {
        var result = service.exportData();
        assertThat(result.path()).isNotNull();
        assertThat(Path.of(result.path())).isDirectory();
    }

    private static void deleteIfExists(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (var walk = Files.walk(dir)) {
                walk.sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); }
                        catch (IOException e) { /* ignore */ }
                    });
            }
        }
    }
}
