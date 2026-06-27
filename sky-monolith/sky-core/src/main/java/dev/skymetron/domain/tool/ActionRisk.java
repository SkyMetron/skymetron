package dev.skymetron.domain.tool;

/**
 * Risk level of a proposed action.
 */
public enum ActionRisk {
    READ_ONLY,
    NETWORK_EGRESS,
    FILE_SYSTEM,
    DESTRUCTIVE,
    PRODUCTION
}
