-- trace_entries: Brain Trace — persistent log of all system events for observability.
-- Consumed by TraceAgent and exposed via /api/trace/timeline.

CREATE TABLE IF NOT EXISTS trace_entries (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    UUID NOT NULL,
    event_type  VARCHAR(50) NOT NULL,
    trace_id    UUID NOT NULL,
    agent_id    VARCHAR(100),
    payload     JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for timeline queries (most recent first).
CREATE INDEX IF NOT EXISTS idx_trace_created_at ON trace_entries (created_at DESC);

-- Index for per-agent trace queries.
CREATE INDEX IF NOT EXISTS idx_trace_agent_id ON trace_entries (agent_id);

-- Index for correlation by trace_id.
CREATE INDEX IF NOT EXISTS idx_trace_trace_id ON trace_entries (trace_id);

-- Index for filtering by event type.
CREATE INDEX IF NOT EXISTS idx_trace_event_type ON trace_entries (event_type);
