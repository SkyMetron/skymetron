package dev.skymetron.application.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
public class ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);
    private static final Path CONFIG_DIR = Paths.get(System.getProperty("user.home"), ".skymetron");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.json");
    private final ObjectMapper objectMapper;

    public ConfigService() {
        this.objectMapper = new ObjectMapper();
    }

    public void saveConfig(Map<String, Object> config) throws IOException {
        Files.createDirectories(CONFIG_DIR);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(CONFIG_FILE.toFile(), config);
        log.info("Config saved to {}", CONFIG_FILE);
    }

    public Map<String, Object> loadConfig() throws IOException {
        if (Files.exists(CONFIG_FILE)) {
            return objectMapper.readValue(CONFIG_FILE.toFile(), Map.class);
        }
        return Map.of();
    }

    public boolean configExists() {
        return Files.exists(CONFIG_FILE);
    }

    public void updateConfig(String key, Object value) throws IOException {
        Map<String, Object> config = loadConfig();
        if (config instanceof ObjectNode) {
            ((ObjectNode) config).put(key, value.toString());
        } else {
            if (config instanceof java.util.HashMap) {
                ((java.util.HashMap<String, Object>) config).put(key, value);
            }
        }
        saveConfig(config);
    }

    public void clearConfig() throws IOException {
        Files.deleteIfExists(CONFIG_FILE);
        log.info("Config cleared");
    }

    public String getConfigDir() {
        return CONFIG_DIR.toString();
    }

    public String getConfigFilePath() {
        return CONFIG_FILE.toString();
    }
}
