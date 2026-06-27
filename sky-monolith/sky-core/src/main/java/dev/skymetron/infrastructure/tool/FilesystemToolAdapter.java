package dev.skymetron.infrastructure.tool;

import dev.skymetron.application.usecase.AgentSafetyPolicy;
import dev.skymetron.domain.tool.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Filesystem tool adapter — list, read, and write files.
 *
 * <p>All actions pass through {@link AgentSafetyPolicy} before execution.
 * Write operations are evaluated as {@code FILE_SYSTEM} risk (sensitive paths
 * denied); read operations are {@code READ_ONLY}.
 */
@Component
public class FilesystemToolAdapter implements Tool {

    private static final Logger log = LoggerFactory.getLogger(FilesystemToolAdapter.class);

    private final AgentSafetyPolicy safetyPolicy;

    public FilesystemToolAdapter(AgentSafetyPolicy safetyPolicy) {
        this.safetyPolicy = safetyPolicy;
    }

    @Override
    public String name() { return "filesystem"; }

    @Override
    public String description() { return "List, read, and write files on the local filesystem."; }

    @Override
    public List<ToolParameter> parameters() {
        return List.of(
                ToolParameter.required("action", "string", "One of: list, read, write"),
                ToolParameter.required("path", "string", "File or directory path"),
                ToolParameter.optional("content", "string", "Content to write (for write action)")
        );
    }

    @Override
    public ActionRisk defaultRisk() { return ActionRisk.FILE_SYSTEM; }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        String action = String.valueOf(params.getOrDefault("action", "")).toLowerCase();
        String pathStr = String.valueOf(params.getOrDefault("path", ""));

        if (pathStr.isBlank()) {
            return ToolResult.failure("Path is required", ActionRisk.READ_ONLY);
        }

        Path path = Paths.get(pathStr);

        return switch (action) {
            case "list" -> listFiles(path);
            case "read" -> readFile(path);
            case "write" -> writeFile(path, String.valueOf(params.getOrDefault("content", "")));
            default -> ToolResult.failure("Unknown action: " + action + ". Use: list, read, write", ActionRisk.READ_ONLY);
        };
    }

    private ToolResult listFiles(Path dir) {
        Action action = Action.fileSystem("list directory: " + dir, dir.toString());
        PolicyDecision decision = safetyPolicy.evaluate(action);
        if (!decision.isAllowed()) {
            return ToolResult.denied(decision.reason());
        }

        if (!Files.isDirectory(dir)) {
            return ToolResult.failure("Not a directory: " + dir, ActionRisk.READ_ONLY);
        }

        try (Stream<Path> stream = Files.list(dir)) {
            String listing = stream
                    .limit(200)
                    .map(p -> (Files.isDirectory(p) ? "[DIR]  " : "       ") + p.getFileName())
                    .collect(Collectors.joining("\n"));
            log.info("Filesystem list: {} ({} entries)", dir, listing.split("\n").length);
            return ToolResult.success(listing, ActionRisk.READ_ONLY);
        } catch (IOException e) {
            return ToolResult.failure("Failed to list: " + e.getMessage(), ActionRisk.READ_ONLY);
        }
    }

    private ToolResult readFile(Path file) {
        Action action = Action.fileSystem("read file: " + file, file.toString());
        PolicyDecision decision = safetyPolicy.evaluate(action);
        if (!decision.isAllowed()) {
            return ToolResult.denied(decision.reason());
        }

        if (!Files.isRegularFile(file)) {
            return ToolResult.failure("Not a file: " + file, ActionRisk.READ_ONLY);
        }

        try {
            String content = Files.readString(file);
            if (content.length() > 10000) {
                content = content.substring(0, 10000) + "\n... (truncated, " + content.length() + " chars total)";
            }
            log.info("Filesystem read: {} ({} chars)", file, content.length());
            return ToolResult.success(content, ActionRisk.READ_ONLY);
        } catch (IOException e) {
            return ToolResult.failure("Failed to read: " + e.getMessage(), ActionRisk.READ_ONLY);
        }
    }

    private ToolResult writeFile(Path file, String content) {
        Action action = Action.fileSystem("write file: " + file, file.toString());
        PolicyDecision decision = safetyPolicy.evaluate(action);
        if (!decision.isAllowed()) {
            return ToolResult.denied(decision.reason());
        }

        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, content);
            log.info("Filesystem write: {} ({} chars)", file, content.length());
            return ToolResult.success("Written " + content.length() + " chars to " + file, ActionRisk.FILE_SYSTEM);
        } catch (IOException e) {
            return ToolResult.failure("Failed to write: " + e.getMessage(), ActionRisk.FILE_SYSTEM);
        }
    }
}
