package dev.skymetron.application.usecase;

import dev.skymetron.domain.tool.Action;
import dev.skymetron.domain.tool.PolicyDecision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AgentSafetyPolicy tests")
class AgentSafetyPolicyTest {

    private final AgentSafetyPolicy policy = new AgentSafetyPolicy();

    @Test
    @DisplayName("READ_ONLY actions are allowed")
    void readOnlyAllowed() {
        PolicyDecision decision = policy.evaluate(Action.readOnly("list files"));
        assertThat(decision.isAllowed()).isTrue();
        assertThat(decision.type()).isEqualTo(PolicyDecision.DecisionType.ALLOW);
    }

    @Test
    @DisplayName("DESTRUCTIVE actions require confirmation")
    void destructiveRequiresConfirmation() {
        PolicyDecision decision = policy.evaluate(Action.destructive("delete file"));
        assertThat(decision.type()).isEqualTo(PolicyDecision.DecisionType.REQUIRE_CONFIRMATION);
    }

    @Test
    @DisplayName("PRODUCTION actions require double confirmation")
    void productionRequiresDoubleConfirmation() {
        PolicyDecision decision = policy.evaluate(Action.production("deploy to prod"));
        assertThat(decision.type()).isEqualTo(PolicyDecision.DecisionType.REQUIRE_DOUBLE_CONFIRMATION);
    }

    @Test
    @DisplayName("NETWORK_EGRESS to localhost is allowed")
    void networkLocalhostAllowed() {
        PolicyDecision decision = policy.evaluate(Action.network("call API", "http://localhost:8080/api"));
        assertThat(decision.isAllowed()).isTrue();
    }

    @Test
    @DisplayName("NETWORK_EGRESS to external requires confirmation")
    void networkExternalRequiresConfirmation() {
        PolicyDecision decision = policy.evaluate(Action.network("call external", "https://api.example.com"));
        assertThat(decision.type()).isEqualTo(PolicyDecision.DecisionType.REQUIRE_CONFIRMATION);
    }

    @Test
    @DisplayName("NETWORK_EGRESS without target is denied")
    void networkNoTargetDenied() {
        PolicyDecision decision = policy.evaluate(new Action("network call", dev.skymetron.domain.tool.ActionRisk.NETWORK_EGRESS, null));
        assertThat(decision.type()).isEqualTo(PolicyDecision.DecisionType.DENY);
    }

    @Test
    @DisplayName("FILE_SYSTEM to safe path is allowed")
    void fileSystemSafePathAllowed() {
        PolicyDecision decision = policy.evaluate(Action.fileSystem("read file", "/home/user/project/file.txt"));
        assertThat(decision.isAllowed()).isTrue();
    }

    @Test
    @DisplayName("FILE_SYSTEM to sensitive path is denied")
    void fileSystemSensitivePathDenied() {
        PolicyDecision decision = policy.evaluate(Action.fileSystem("read ssh key", "~/.ssh/id_rsa"));
        assertThat(decision.type()).isEqualTo(PolicyDecision.DecisionType.DENY);
    }

    @Test
    @DisplayName("FILE_SYSTEM with path traversal is denied")
    void fileSystemTraversalDenied() {
        PolicyDecision decision = policy.evaluate(Action.fileSystem("read", "../../etc/passwd"));
        assertThat(decision.type()).isEqualTo(PolicyDecision.DecisionType.DENY);
    }
}
