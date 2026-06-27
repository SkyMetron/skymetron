package dev.skymetron.domain.execution;

import dev.skymetron.domain.tool.TaskType;
import java.util.Set;

/**
 * Declares what an agent can handle.
 */
public record AgentCapabilities(
        Set<Intent> supportedIntents,
        Set<TaskType> supportedTaskTypes,
        Set<String> tools
) {
    public boolean supports(Intent intent) {
        return supportedIntents.contains(intent);
    }
}
