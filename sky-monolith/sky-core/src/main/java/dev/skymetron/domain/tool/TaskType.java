package dev.skymetron.domain.tool;

/**
 * Classification of LLM task types — used by the CeoAgent to route
 * to the most appropriate provider.
 */
public enum TaskType {
    CODE_GENERATION,
    LONG_ANALYSIS,
    WEB_RESEARCH,
    FAST_RESPONSE,
    LONG_CONTEXT,
    OFFLINE,
    REFACTORING,
    DOCUMENTATION,
    SECURITY,
    QA_TESTING,
    MEMORY_DEDUP,
    VAULT_CONSOLIDATION,
    DOCS_RESEARCH,
    CODE_RESEARCH,
    PAPER_RESEARCH,
    GENERAL
}
