-- Enable pgvector extension for vector similarity search.
-- Requires PostgreSQL 16 + pgvector (provided by pgvector/pgvector image).
CREATE EXTENSION IF NOT EXISTS vector;

-- Validate extension is active.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_extension WHERE extname = 'vector'
    ) THEN
        RAISE EXCEPTION 'pgvector extension is required but could not be installed';
    END IF;
END $$;
