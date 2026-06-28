package dev.skymetron.application.bootstrap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConfigService tests")
class ConfigServiceTest {

    private ConfigService service;
    private Path configDir;
    private Path configFile;

    @BeforeEach
    void setUp() throws Exception {
        service = new ConfigService();
        configDir = Path.of(System.getProperty("user.home"), ".skymetron");
        configFile = configDir.resolve("config.json");
        Files.deleteIfExists(configFile);
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(configFile);
    }

    @Test
    @DisplayName("configExists returns false when no config")
    void configNotExists() {
        assertThat(service.configExists()).isFalse();
    }

    @Test
    @DisplayName("save and load round-trip")
    void saveAndLoad() throws Exception {
        service.saveConfig(Map.of("key1", "value1", "key2", 42));
        assertThat(service.configExists()).isTrue();

        Map<String, Object> loaded = service.loadConfig();
        assertThat(loaded).containsEntry("key1", "value1");
    }

    @Test
    @DisplayName("loadConfig returns empty map when no config exists")
    void loadConfigEmpty() throws Exception {
        Map<String, Object> loaded = service.loadConfig();
        assertThat(loaded).isEmpty();
    }

    @Test
    @DisplayName("clearConfig removes the config file")
    void clearConfig() throws Exception {
        service.saveConfig(Map.of("key", "value"));
        assertThat(service.configExists()).isTrue();

        service.clearConfig();
        assertThat(service.configExists()).isFalse();
    }

    @Test
    @DisplayName("getConfigDir returns non-null path")
    void getConfigDir() {
        assertThat(service.getConfigDir()).isNotNull().contains(".skymetron");
    }

    @Test
    @DisplayName("getConfigFilePath returns non-null path")
    void getConfigFilePath() {
        assertThat(service.getConfigFilePath()).isNotNull().contains("config.json");
    }

    @Test
    @DisplayName("saveConfig overwrites existing config")
    void saveOverwrites() throws Exception {
        service.saveConfig(Map.of("key", "old"));
        service.saveConfig(Map.of("key", "new"));
        assertThat(service.loadConfig()).containsEntry("key", "new");
    }
}
