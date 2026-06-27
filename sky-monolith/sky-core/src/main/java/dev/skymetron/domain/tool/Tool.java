package dev.skymetron.domain.tool;

import java.util.List;
import java.util.Map;

/**
 * A tool that an agent can execute (filesystem, git, docker, web search, etc.).
 *
 * <p>Each tool declares its name, the parameters it accepts, and the risk
 * level of its actions. Execution is mediated by {@code AgentSafetyPolicy}.
 */
public interface Tool {

    /** Unique tool name (e.g. "filesystem.list", "git.status"). */
    String name();

    /** Human-readable description of what the tool does. */
    String description();

    /** Parameters this tool accepts. */
    List<ToolParameter> parameters();

    /** Default risk level for this tool's actions. */
    ActionRisk defaultRisk();

    /**
     * Execute the tool with the given parameters.
     *
     * @return the result of the execution
     */
    ToolResult execute(Map<String, Object> parameters);

    /** A single parameter declaration. */
    record ToolParameter(
            String name,
            String type,
            String description,
            boolean required
    ) {
        public static ToolParameter required(String name, String type, String description) {
            return new ToolParameter(name, type, description, true);
        }

        public static ToolParameter optional(String name, String type, String description) {
            return new ToolParameter(name, type, description, false);
        }
    }
}
