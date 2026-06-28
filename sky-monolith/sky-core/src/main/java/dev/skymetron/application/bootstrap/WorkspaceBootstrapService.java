package dev.skymetron.application.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class WorkspaceBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceBootstrapService.class);
    private static final Path HOME = Paths.get(System.getProperty("user.home"));
    private static final Path WORKSPACE_ROOT = HOME.resolve("SkyMetron");
    private static final Path CONFIG_DIR = HOME.resolve(".skymetron");
    private static final List<String> MAINTAINER_REPOS = List.of(
        "git@github.com:SkyMetron/skymetron.git",
        "git@github.com:SkyMetron/sky-vault.git",
        "git@github.com:SkyMetron/skymetron-ai-services.git",
        "git@github.com:SkyMetron/skymetron-monitoring.git",
        "git@github.com:SkyMetron/skymetron-deployment.git",
        "git@github.com:SkyMetron/skymetron-docs.git"
    );

    public WorkspaceConfig buildConfig(String username, boolean isMaintainer) {
        Path workspacePath = WORKSPACE_ROOT;
        Path vaultPath = isMaintainer
            ? workspacePath.resolve("sky-vault")
            : workspacePath.resolve("vault");
        Path configPath = CONFIG_DIR.resolve("config.json");

        List<String> repos = isMaintainer ? MAINTAINER_REPOS : List.of();

        return new WorkspaceConfig(
            username,
            isMaintainer ? "DEVELOPER" : "USER",
            workspacePath,
            vaultPath,
            configPath,
            repos,
            Files.exists(workspacePath.resolve(".env")),
            detectDocker(),
            detectJava(),
            detectGit(),
            detectOllama(),
            detectPostgres(),
            detectRabbitMq(),
            detectRedis()
        );
    }

    public void ensureWorkspace(String username, boolean isMaintainer) throws IOException {
        Files.createDirectories(WORKSPACE_ROOT);
        Files.createDirectories(CONFIG_DIR);

        if (isMaintainer) {
            createMaintainerStructure();
        } else {
            createUserStructure();
        }

        log.info("Workspace created at {} for user {} (type: {})",
            WORKSPACE_ROOT, username, isMaintainer ? "DEVELOPER" : "USER");
    }

    private void createMaintainerStructure() throws IOException {
        Files.createDirectories(WORKSPACE_ROOT);
    }

    private void createUserStructure() throws IOException {
        for (String dir : List.of("workspace", "vault", "projects", "config", "logs", "data", "cache")) {
            Files.createDirectories(WORKSPACE_ROOT.resolve(dir));
        }
        for (String dir : List.of("knowledge", "projects", "skills", "research", "notes", "memories")) {
            Files.createDirectories(WORKSPACE_ROOT.resolve("vault").resolve(dir));
        }
    }

    private boolean detectDocker() {
        return isExecutableAvailable("docker");
    }

    private boolean detectJava() {
        return isExecutableAvailable("java");
    }

    private boolean detectGit() {
        return isExecutableAvailable("git");
    }

    private boolean detectOllama() {
        return isExecutableAvailable("ollama");
    }

    private boolean detectPostgres() {
        return isExecutableAvailable("psql");
    }

    private boolean detectRabbitMq() {
        return isExecutableAvailable("rabbitmqctl") || isExecutableAvailable("rabbitmq-server");
    }

    private boolean detectRedis() {
        return isExecutableAvailable("redis-cli");
    }

    private boolean isExecutableAvailable(String name) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                System.getProperty("os.name").toLowerCase().contains("win") ? "where" : "which",
                name
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            int exit = p.waitFor();
            return exit == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
