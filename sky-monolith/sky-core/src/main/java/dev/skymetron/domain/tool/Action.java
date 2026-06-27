package dev.skymetron.domain.tool;

/**
 * A proposed action that needs safety evaluation.
 */
public record Action(
        String description,
        ActionRisk risk,
        String target
) {
    public static Action readOnly(String description) {
        return new Action(description, ActionRisk.READ_ONLY, null);
    }

    public static Action fileSystem(String description, String path) {
        return new Action(description, ActionRisk.FILE_SYSTEM, path);
    }

    public static Action network(String description, String url) {
        return new Action(description, ActionRisk.NETWORK_EGRESS, url);
    }

    public static Action destructive(String description) {
        return new Action(description, ActionRisk.DESTRUCTIVE, null);
    }

    public static Action production(String description) {
        return new Action(description, ActionRisk.PRODUCTION, null);
    }
}
