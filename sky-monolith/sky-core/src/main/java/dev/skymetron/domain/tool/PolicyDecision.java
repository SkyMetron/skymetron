package dev.skymetron.domain.tool;

/**
 * Decision returned by {@link dev.skymetron.application.usecase.AgentSafetyPolicy}.
 */
public record PolicyDecision(
        DecisionType type,
        String reason,
        Action action
) {
    public enum DecisionType { ALLOW, REQUIRE_CONFIRMATION, REQUIRE_DOUBLE_CONFIRMATION, DENY }

    public static PolicyDecision allow() {
        return new PolicyDecision(DecisionType.ALLOW, "Action is safe", null);
    }

    public static PolicyDecision requireConfirmation(Action action) {
        return new PolicyDecision(DecisionType.REQUIRE_CONFIRMATION, "Action requires user confirmation", action);
    }

    public static PolicyDecision requireDoubleConfirmation(Action action) {
        return new PolicyDecision(DecisionType.REQUIRE_DOUBLE_CONFIRMATION, "Action requires double confirmation", action);
    }

    public static PolicyDecision deny(Action action, String reason) {
        return new PolicyDecision(DecisionType.DENY, reason, action);
    }

    public boolean isAllowed() {
        return type == DecisionType.ALLOW;
    }
}
