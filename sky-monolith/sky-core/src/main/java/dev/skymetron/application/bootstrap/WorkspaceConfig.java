package dev.skymetron.application.bootstrap;

import java.nio.file.Path;
import java.util.List;

public record WorkspaceConfig(
    String username,
    String workspaceType,
    Path workspacePath,
    Path vaultPath,
    Path configPath,
    List<String> reposToClone,
    boolean envFileExists,
    boolean dockerDetected,
    boolean javaDetected,
    boolean gitDetected,
    boolean ollamaDetected,
    boolean postgresDetected,
    boolean rabbitMqDetected,
    boolean redisDetected
) {
    public boolean isMaintainer() {
        return "DEVELOPER".equals(workspaceType);
    }

    public boolean isComplete() {
        return dockerDetected && javaDetected && gitDetected
            && envFileExists;
    }
}
