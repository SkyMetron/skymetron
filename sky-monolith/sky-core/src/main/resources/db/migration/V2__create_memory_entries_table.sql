-- memory_entries: the core of the SkyMetron Vault.
-- Stores facts, session context, project knowledge, skill embeddings with
-- nomic-embed-text vectors (768 dimensions) for semantic similarity search.

CREATE TABLE IF NOT EXISTS memory_entries (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content     TEXT NOT NULL,
    embedding   vector(768),
    metadata    JSONB NOT NULL DEFAULT '{}'::jsonb,
    source      VARCHAR(100),
    confidence  DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    merged_into UUID REFERENCES memory_entries(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index metadata type filter (user_facts, project_knowledge, session_context, skill_embeddings).
CREATE INDEX IF NOT EXISTS idx_memory_metadata_type
    ON memory_entries ((metadata->>'type'));

-- Index active (non-merged) entries.
CREATE INDEX IF NOT EXISTS idx_memory_active
    ON memory_entries (merged_into)
    WHERE merged_into IS NULL;

-- Full-text search on content.
CREATE INDEX IF NOT EXISTS idx_memory_content_fts
    ON memory_entries USING gin (to_tsvector('simple', content));

-- HNSW index for approximate nearest-neighbor search (cosine distance).
-- m=16, ef_construction=64 — sweet spot for <100k vectors of 768 dims.
CREATE INDEX IF NOT EXISTS idx_memory_embeddings_hnsw
    ON memory_entries
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- Updated-at trigger.
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_memory_entries_updated_at ON memory_entries;
CREATE TRIGGER trg_memory_entries_updated_at
    BEFORE UPDATE ON memory_entries
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
