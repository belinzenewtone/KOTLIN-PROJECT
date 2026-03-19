-- ================================================
-- BELTECH Supabase Migration v6
-- Add event completion status for calendar parity
-- ================================================

ALTER TABLE events
    ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'PENDING';

CREATE INDEX IF NOT EXISTS idx_events_status ON events(status);
