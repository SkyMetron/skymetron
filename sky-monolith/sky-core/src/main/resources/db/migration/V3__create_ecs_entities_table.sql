-- ecs_entities: stores ECS entities and their components (JSONB) for dynamic composition.
-- Used by agents, sessions, and missions that need flexible component attachment.

CREATE TABLE IF NOT EXISTS ecs_entities (
    id          UUID PRIMARY KEY,
    components  JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for querying by component presence (e.g. has IdentityComponent).
CREATE INDEX IF NOT EXISTS idx_ecs_entities_components
    ON ecs_entities USING gin (components);

-- Updated-at trigger (reuse function from V2 if exists, else create).
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_ecs_entities_updated_at ON ecs_entities;
CREATE TRIGGER trg_ecs_entities_updated_at
    BEFORE UPDATE ON ecs_entities
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
