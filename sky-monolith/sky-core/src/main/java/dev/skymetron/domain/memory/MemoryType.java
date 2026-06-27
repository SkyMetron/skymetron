package dev.skymetron.domain.memory;

/**
 * Classification of memory entries stored in the Vault.
 *
 * <p>Used as a filter in {@code metadata->>'type'} for collection-level queries.
 */
public enum MemoryType {

    /** Facts about the user (preferences, identity, history). */
    USER_FACTS,

    /** Knowledge about projects (architecture, decisions, ADRs). */
    PROJECT_KNOWLEDGE,

    /** Transient context from sessions (summaries, recent interactions). */
    SESSION_CONTEXT,

    /** Embeddings of skills (catalogued capabilities). */
    SKILL_EMBEDDINGS
}
