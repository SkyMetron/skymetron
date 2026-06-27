package dev.skymetron.domain.execution;

/**
 * Classification of what the user/CEO wants to do.
 *
 * <p>Used by the CeoAgent to route to the appropriate downstream agent.
 */
public enum Intent {
    MEMORY_QUERY,
    MEMORY_STORE,
    TOOL_EXECUTION,
    RESEARCH,
    GENERAL_CHAT,
    CODE_HELP,
    SYSTEM_CONTROL,
    SKILL_MANAGEMENT,
    QA_TEST,
    SECURITY_ANALYSIS,
    RESEARCH_SWARM
}
