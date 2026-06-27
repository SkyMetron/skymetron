package dev.skymetron.domain.tool;

/**
 * Result of a tool execution.
 */
public record ToolResult(
        boolean success,
        String output,
        String error,
        ActionRisk riskLevel
) {
    public static ToolResult success(String output, ActionRisk risk) {
        return new ToolResult(true, output, null, risk);
    }

    public static ToolResult failure(String error, ActionRisk risk) {
        return new ToolResult(false, null, error, risk);
    }

    public static ToolResult denied(String reason) {
        return new ToolResult(false, null, "DENIED by SafetyPolicy: " + reason, ActionRisk.READ_ONLY);
    }
}
