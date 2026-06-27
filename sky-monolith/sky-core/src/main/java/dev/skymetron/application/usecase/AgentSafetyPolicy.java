package dev.skymetron.application.usecase;

import dev.skymetron.domain.tool.Action;
import dev.skymetron.domain.tool.ActionRisk;
import dev.skymetron.domain.tool.PolicyDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Evaluates proposed actions for safety before execution.
 *
 * <p>Rule 4 (Absolute Rules): No agent acts without a SafetyPolicy evaluation.
 * Read-only actions are allowed; destructive and production actions require
 * confirmation; file system and network egress are evaluated by policy.
 */
@Component
public class AgentSafetyPolicy {

    private static final Logger log = LoggerFactory.getLogger(AgentSafetyPolicy.class);

    public PolicyDecision evaluate(Action action) {
        log.debug("Evaluating action: {} risk={} target={}", action.description(), action.risk(), action.target());
        return switch (action.risk()) {
            case READ_ONLY -> PolicyDecision.allow();
            case DESTRUCTIVE -> PolicyDecision.requireConfirmation(action);
            case PRODUCTION -> PolicyDecision.requireDoubleConfirmation(action);
            case NETWORK_EGRESS -> evaluateNetworkPolicy(action);
            case FILE_SYSTEM -> evaluateFileSystemPolicy(action);
        };
    }

    private PolicyDecision evaluateNetworkPolicy(Action action) {
        if (action.target() == null || action.target().isBlank()) {
            return PolicyDecision.deny(action, "Network egress requires a target URL");
        }
        String lower = action.target().toLowerCase();
        if (lower.startsWith("http://localhost") || lower.startsWith("https://localhost")
                || lower.startsWith("http://127.0.0.1") || lower.startsWith("https://127.0.0.1")) {
            return PolicyDecision.allow();
        }
        return PolicyDecision.requireConfirmation(action);
    }

    private PolicyDecision evaluateFileSystemPolicy(Action action) {
        if (action.target() == null || action.target().isBlank()) {
            return PolicyDecision.deny(action, "File system action requires a target path");
        }
        String lower = action.target().toLowerCase();
        if (lower.contains("..") || lower.contains(".ssh") || lower.contains(".aws")
                || lower.contains("/etc/") || lower.contains("\\etc\\")
                || lower.contains("c:\\windows") || lower.contains("/root/")) {
            return PolicyDecision.deny(action, "Access to sensitive path denied: " + action.target());
        }
        return PolicyDecision.allow();
    }
}
