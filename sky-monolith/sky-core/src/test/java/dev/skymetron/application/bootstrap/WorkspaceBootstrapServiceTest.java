package dev.skymetron.application.bootstrap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WorkspaceBootstrapService tests")
class WorkspaceBootstrapServiceTest {

    private WorkspaceBootstrapService service;

    @BeforeEach
    void setUp() {
        service = new WorkspaceBootstrapService();
    }

    @Test
    @DisplayName("buildConfig returns DEVELOPER type for maintainer")
    void buildConfigForMaintainer() {
        WorkspaceConfig config = service.buildConfig("Joao-Aschenbrenner", true);

        assertThat(config.workspaceType()).isEqualTo("DEVELOPER");
        assertThat(config.username()).isEqualTo("Joao-Aschenbrenner");
        assertThat(config.reposToClone()).isNotEmpty();
        assertThat(config.isMaintainer()).isTrue();
    }

    @Test
    @DisplayName("buildConfig returns USER type for non-maintainer")
    void buildConfigForUser() {
        WorkspaceConfig config = service.buildConfig("some-user", false);

        assertThat(config.workspaceType()).isEqualTo("USER");
        assertThat(config.reposToClone()).isEmpty();
        assertThat(config.isMaintainer()).isFalse();
    }

    @Test
    @DisplayName("buildConfig user vault path ends with vault")
    void buildConfigUserVaultPath() {
        WorkspaceConfig config = service.buildConfig("some-user", false);
        assertThat(config.vaultPath().toString()).endsWith("vault");
    }

    @Test
    @DisplayName("buildConfig maintainer vault path ends with sky-vault")
    void buildConfigMaintainerVaultPath() {
        WorkspaceConfig config = service.buildConfig("Joao-Aschenbrenner", true);
        assertThat(config.vaultPath().toString()).endsWith("sky-vault");
    }

    @Test
    @DisplayName("ensureWorkspace creates directories for user in real home")
    void ensureWorkspaceForUser() throws IOException {
        Path userHome = Path.of(System.getProperty("user.home"));
        Path skyMetronDir = userHome.resolve("SkyMetron");
        boolean existed = Files.exists(skyMetronDir);
        try {
            service.ensureWorkspace("test-user", false);
            assertThat(skyMetronDir).isDirectory();
            assertThat(skyMetronDir.resolve("workspace")).isDirectory();
            assertThat(skyMetronDir.resolve("vault")).isDirectory();
        } finally {
            if (!existed) {
                deleteDirectory(skyMetronDir);
            }
        }
    }

    @Test
    @DisplayName("ensureWorkspace creates directories for maintainer in real home")
    void ensureWorkspaceForMaintainer() throws IOException {
        Path userHome = Path.of(System.getProperty("user.home"));
        Path skyMetronDir = userHome.resolve("SkyMetron");
        boolean existed = Files.exists(skyMetronDir);
        try {
            service.ensureWorkspace("Joao-Aschenbrenner", true);
            assertThat(skyMetronDir).isDirectory();
        } finally {
            if (!existed) {
                deleteDirectory(skyMetronDir);
            }
        }
    }

    @Test
    @DisplayName("isComplete returns false when tools are missing")
    void isCompleteFalse() {
        WorkspaceConfig config = service.buildConfig("user", false);
        assertThat(config.isComplete()).isFalse();
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (var walk = java.nio.file.Files.walk(dir)) {
                walk.sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); }
                        catch (IOException e) { /* ignore */ }
                    });
            }
        }
    }
}
