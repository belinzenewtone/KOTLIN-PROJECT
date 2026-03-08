-- ================================================
-- BELTECH App — Supabase Migration v4
-- Purpose: enforce user-scoped identity keys for sync tables
-- Date: 2026-03-08
-- ================================================

BEGIN;

-- Transactions: move from PRIMARY KEY (id) to PRIMARY KEY (user_id, id)
ALTER TABLE IF EXISTS public.transactions
    DROP CONSTRAINT IF EXISTS transactions_pkey;
ALTER TABLE IF EXISTS public.transactions
    ADD CONSTRAINT transactions_pkey PRIMARY KEY (user_id, id);

-- Tasks
ALTER TABLE IF EXISTS public.tasks
    DROP CONSTRAINT IF EXISTS tasks_pkey;
ALTER TABLE IF EXISTS public.tasks
    ADD CONSTRAINT tasks_pkey PRIMARY KEY (user_id, id);

-- Events
ALTER TABLE IF EXISTS public.events
    DROP CONSTRAINT IF EXISTS events_pkey;
ALTER TABLE IF EXISTS public.events
    ADD CONSTRAINT events_pkey PRIMARY KEY (user_id, id);

-- Merchant categories
ALTER TABLE IF EXISTS public.merchant_categories
    DROP CONSTRAINT IF EXISTS merchant_categories_pkey;
ALTER TABLE IF EXISTS public.merchant_categories
    ADD CONSTRAINT merchant_categories_pkey PRIMARY KEY (user_id, id);

-- Keep/restore secondary indexes used by API filters and sync queries.
CREATE INDEX IF NOT EXISTS idx_transactions_user_id_id
    ON public.transactions (user_id, id);
CREATE INDEX IF NOT EXISTS idx_tasks_user_id_id
    ON public.tasks (user_id, id);
CREATE INDEX IF NOT EXISTS idx_events_user_id_id
    ON public.events (user_id, id);
CREATE INDEX IF NOT EXISTS idx_merchant_categories_user_id_id
    ON public.merchant_categories (user_id, id);

COMMIT;

-- Optional verification (run after migration):
-- SELECT conname, conrelid::regclass, pg_get_constraintdef(oid)
-- FROM pg_constraint
-- WHERE conrelid::regclass::text IN ('public.transactions', 'public.tasks', 'public.events', 'public.merchant_categories')
--   AND contype = 'p';
