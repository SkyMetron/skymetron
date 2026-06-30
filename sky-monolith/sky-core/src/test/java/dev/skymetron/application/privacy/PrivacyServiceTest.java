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
    private Path configFile;
    private Path workspaceRoot;

    @BeforeEach
    void setUp() throws IOException {
        service = new PrivacyService();
        configDir = Path.of(System.getProperty("user.home"), ".skymetron");
        configFile = configDir.resolve("config.json");
        workspaceRoot = Path.of(System.getProperty("user.home"), "SkyMetron");
        Files.deleteIfExists(configFile);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(configDir.resolve(".terms-accepted"));
        Files.deleteIfExists(configDir.resolve(".lgpd-accepted"));
        Files.deleteIfExists(configFile);
        Files.deleteIfExists(workspaceRoot.resolve(".env"));
        deleteIfExists(workspaceRoot.resolve("cache"));
        deleteIfExists(workspaceRoot.resolve("logs"));
    }

    @Test
    @DisplayName("hasAcceptedTerms returns false initially")
    void termsNotAccepted() {
        assertThat(service.hasAcceptedTerms()).isFalse();
    }

    @Test
    @DisplayName("acceptTerms stores current terms version")
    void acceptTerms() throws IOException {
        service.acceptTerms("test-user");
        assertThat(service.hasAcceptedTerms()).isTrue();
        assertThat(service.legalStatus()).containsEntry("termsVersion", PrivacyService.TERMS_VERSION);
        assertThat(service.legalStatus()).containsEntry("acceptedByGitHubUser", "test-user");
    }

    @Test
    @DisplayName("hasAcceptedLgpd returns false initially")
    void lgpdNotAccepted() {
        assertThat(service.hasAcceptedLgpd()).isFalse();
    }

    @Test
    @DisplayName("acceptLgpd stores current privacy version")
    void acceptLgpd() throws IOException {
        service.acceptLgpd("test-user");
        assertThat(service.hasAcceptedLgpd()).isTrue();
        assertThat(service.legalStatus()).containsEntry("privacyVersion", PrivacyService.PRIVACY_VERSION);
    }

    @Test
    @DisplayName("legal acceptance requires current versions")
    void legalAcceptanceRequiresCurrentVersions() throws IOException {
        Files.createDirectories(configDir);
        Files.writeString(configFile, "{\"termsAccepted\":true,\"lgpdAccepted\":true,\"termsVersion\":\"old\",\"privacyVersion\":\"old\"}");

        assertThat(service.hasAcceptedTerms()).isFalse();
        assertThat(service.hasAcceptedLgpd()).isFalse();
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

    @Test
    @DisplayName("maskSecrets masks known provider token formats")
    void maskSecrets() {
        String raw = String.join("\n",
            "OPENAI=sk-1234567890abcdef",
            "GITHUB=ghp_1234567890abcdef",
            "GITHUB_PAT=github_pat_1234567890abcdef",
            "NVIDIA=nvapi-1234567890abcdef",
            "GROQ=gsk_1234567890abcdef",
            "GOOGLE=AIza1234567890abcdef",
            "OPENROUTER=sk-or-1234567890abcdef");

        String masked = service.maskSecrets(raw);

        assertThat(masked).contains("sk-***");
        assertThat(masked).contains("ghp_***");
        assertThat(masked).contains("github_pat_***");
        assertThat(masked).contains("nvapi-***");
        assertThat(masked).contains("gsk_***");
        assertThat(masked).contains("AIza***");
        assertThat(masked).contains("sk-or-***");
        assertThat(masked).doesNotContain("1234567890abcdef");
    }

    @Test
    @DisplayName("exportData masks secrets from .env")
    void exportDataMasksSecrets() throws IOException {
        Files.createDirectories(workspaceRoot);
        Files.writeString(workspaceRoot.resolve(".env"), "OPENAI_API_KEY=sk-1234567890abcdef");

        var result = service.exportData();
        String exportedEnv = Files.readString(Path.of(result.path()).resolve(".env"));

        assertThat(exportedEnv).contains("OPENAI_API_KEY=sk-***");
        assertThat(exportedEnv).doesNotContain("1234567890abcdef");
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
