-- ================================================
-- BELTECH App — Supabase Migration v7 (Final Hardening)
-- Purpose: idempotent schema + policy alignment to Kotlin project state
-- Date: 2026-03-08
-- ================================================

BEGIN;

-- ------------------------------------------------
-- 1) Ensure tables exist (for older projects)
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS public.transactions (
    id BIGINT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    amount DOUBLE PRECISION NOT NULL,
    merchant TEXT NOT NULL,
    category TEXT NOT NULL,
    date BIGINT NOT NULL,
    source TEXT DEFAULT 'MPESA',
    transaction_type TEXT DEFAULT 'SENT',
    mpesa_code TEXT,
    raw_sms TEXT,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

CREATE TABLE IF NOT EXISTS public.tasks (
    id BIGINT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT DEFAULT '',
    priority TEXT DEFAULT 'MEDIUM',
    deadline BIGINT,
    status TEXT DEFAULT 'PENDING',
    completed_at BIGINT,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

CREATE TABLE IF NOT EXISTS public.events (
    id BIGINT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT DEFAULT '',
    date BIGINT NOT NULL,
    end_date BIGINT,
    type TEXT DEFAULT 'PERSONAL',
    importance TEXT DEFAULT 'NEUTRAL',
    status TEXT DEFAULT 'PENDING',
    has_reminder BOOLEAN DEFAULT FALSE,
    reminder_minutes_before INTEGER DEFAULT 15,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

CREATE TABLE IF NOT EXISTS public.merchant_categories (
    id BIGINT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    merchant TEXT NOT NULL,
    category TEXT NOT NULL,
    confidence REAL DEFAULT 1.0,
    user_corrected BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS public.budgets (
    id BIGINT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    category TEXT NOT NULL,
    limit_amount DOUBLE PRECISION NOT NULL,
    period TEXT NOT NULL DEFAULT 'MONTHLY',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

CREATE TABLE IF NOT EXISTS public.incomes (
    id BIGINT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    amount DOUBLE PRECISION NOT NULL,
    source TEXT NOT NULL,
    date BIGINT NOT NULL,
    note TEXT DEFAULT '',
    is_recurring BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS public.recurring_rules (
    id BIGINT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    type TEXT NOT NULL,
    cadence TEXT NOT NULL,
    next_run_at BIGINT NOT NULL,
    amount DOUBLE PRECISION,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username TEXT,
    full_name TEXT,
    email TEXT,
    phone TEXT,
    profile_pic_url TEXT,
    member_since BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ------------------------------------------------
-- 2) Ensure required columns exist on legacy tables
-- ------------------------------------------------
ALTER TABLE IF EXISTS public.transactions ADD COLUMN IF NOT EXISTS source TEXT DEFAULT 'MPESA';
ALTER TABLE IF EXISTS public.transactions ADD COLUMN IF NOT EXISTS transaction_type TEXT DEFAULT 'SENT';
ALTER TABLE IF EXISTS public.transactions ADD COLUMN IF NOT EXISTS mpesa_code TEXT;
ALTER TABLE IF EXISTS public.transactions ADD COLUMN IF NOT EXISTS raw_sms TEXT;
ALTER TABLE IF EXISTS public.transactions
    ADD COLUMN IF NOT EXISTS created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;

ALTER TABLE IF EXISTS public.tasks ADD COLUMN IF NOT EXISTS priority TEXT DEFAULT 'MEDIUM';
ALTER TABLE IF EXISTS public.tasks ADD COLUMN IF NOT EXISTS deadline BIGINT;
ALTER TABLE IF EXISTS public.tasks ADD COLUMN IF NOT EXISTS status TEXT DEFAULT 'PENDING';
ALTER TABLE IF EXISTS public.tasks ADD COLUMN IF NOT EXISTS completed_at BIGINT;
ALTER TABLE IF EXISTS public.tasks
    ADD COLUMN IF NOT EXISTS created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;

ALTER TABLE IF EXISTS public.events ADD COLUMN IF NOT EXISTS end_date BIGINT;
ALTER TABLE IF EXISTS public.events ADD COLUMN IF NOT EXISTS type TEXT DEFAULT 'PERSONAL';
ALTER TABLE IF EXISTS public.events ADD COLUMN IF NOT EXISTS importance TEXT DEFAULT 'NEUTRAL';
ALTER TABLE IF EXISTS public.events ADD COLUMN IF NOT EXISTS status TEXT DEFAULT 'PENDING';
ALTER TABLE IF EXISTS public.events ADD COLUMN IF NOT EXISTS has_reminder BOOLEAN DEFAULT FALSE;
ALTER TABLE IF EXISTS public.events ADD COLUMN IF NOT EXISTS reminder_minutes_before INTEGER DEFAULT 15;
ALTER TABLE IF EXISTS public.events
    ADD COLUMN IF NOT EXISTS created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;

ALTER TABLE IF EXISTS public.merchant_categories ADD COLUMN IF NOT EXISTS confidence REAL DEFAULT 1.0;
ALTER TABLE IF EXISTS public.merchant_categories ADD COLUMN IF NOT EXISTS user_corrected BOOLEAN DEFAULT FALSE;

ALTER TABLE IF EXISTS public.budgets ADD COLUMN IF NOT EXISTS period TEXT NOT NULL DEFAULT 'MONTHLY';
ALTER TABLE IF EXISTS public.budgets
    ADD COLUMN IF NOT EXISTS created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;

ALTER TABLE IF EXISTS public.incomes ADD COLUMN IF NOT EXISTS note TEXT DEFAULT '';
ALTER TABLE IF EXISTS public.incomes ADD COLUMN IF NOT EXISTS is_recurring BOOLEAN DEFAULT FALSE;

ALTER TABLE IF EXISTS public.recurring_rules ADD COLUMN IF NOT EXISTS amount DOUBLE PRECISION;
ALTER TABLE IF EXISTS public.recurring_rules ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE IF EXISTS public.recurring_rules
    ADD COLUMN IF NOT EXISTS created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;

ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS username TEXT;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS full_name TEXT;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS email TEXT;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS phone TEXT;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS profile_pic_url TEXT;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS member_since BIGINT;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();

-- Ensure event status is non-null and defaulted.
UPDATE public.events
SET status = 'PENDING'
WHERE status IS NULL;
ALTER TABLE public.events ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE public.events ALTER COLUMN status SET NOT NULL;

-- ------------------------------------------------
-- 3) Enforce composite identity keys used by sync
-- ------------------------------------------------
ALTER TABLE IF EXISTS public.transactions DROP CONSTRAINT IF EXISTS transactions_pkey;
ALTER TABLE IF EXISTS public.transactions ADD CONSTRAINT transactions_pkey PRIMARY KEY (user_id, id);

ALTER TABLE IF EXISTS public.tasks DROP CONSTRAINT IF EXISTS tasks_pkey;
ALTER TABLE IF EXISTS public.tasks ADD CONSTRAINT tasks_pkey PRIMARY KEY (user_id, id);

ALTER TABLE IF EXISTS public.events DROP CONSTRAINT IF EXISTS events_pkey;
ALTER TABLE IF EXISTS public.events ADD CONSTRAINT events_pkey PRIMARY KEY (user_id, id);

ALTER TABLE IF EXISTS public.merchant_categories DROP CONSTRAINT IF EXISTS merchant_categories_pkey;
ALTER TABLE IF EXISTS public.merchant_categories ADD CONSTRAINT merchant_categories_pkey PRIMARY KEY (user_id, id);

ALTER TABLE IF EXISTS public.budgets DROP CONSTRAINT IF EXISTS budgets_pkey;
ALTER TABLE IF EXISTS public.budgets ADD CONSTRAINT budgets_pkey PRIMARY KEY (user_id, id);

ALTER TABLE IF EXISTS public.incomes DROP CONSTRAINT IF EXISTS incomes_pkey;
ALTER TABLE IF EXISTS public.incomes ADD CONSTRAINT incomes_pkey PRIMARY KEY (user_id, id);

ALTER TABLE IF EXISTS public.recurring_rules DROP CONSTRAINT IF EXISTS recurring_rules_pkey;
ALTER TABLE IF EXISTS public.recurring_rules ADD CONSTRAINT recurring_rules_pkey PRIMARY KEY (user_id, id);

ALTER TABLE IF EXISTS public.merchant_categories
    DROP CONSTRAINT IF EXISTS merchant_categories_user_id_merchant_key;
ALTER TABLE IF EXISTS public.merchant_categories
    ADD CONSTRAINT merchant_categories_user_id_merchant_key UNIQUE (user_id, merchant);

-- ------------------------------------------------
-- 4) Index hardening (sync + query performance)
-- ------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_transactions_user ON public.transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_user_id_id ON public.transactions(user_id, id);
CREATE INDEX IF NOT EXISTS idx_transactions_date ON public.transactions(date);
CREATE INDEX IF NOT EXISTS idx_transactions_category ON public.transactions(category);
CREATE INDEX IF NOT EXISTS idx_transactions_merchant ON public.transactions(merchant);

CREATE INDEX IF NOT EXISTS idx_tasks_user ON public.tasks(user_id);
CREATE INDEX IF NOT EXISTS idx_tasks_user_id_id ON public.tasks(user_id, id);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON public.tasks(status);
CREATE INDEX IF NOT EXISTS idx_tasks_deadline ON public.tasks(deadline);
CREATE INDEX IF NOT EXISTS idx_tasks_priority ON public.tasks(priority);

CREATE INDEX IF NOT EXISTS idx_events_user ON public.events(user_id);
CREATE INDEX IF NOT EXISTS idx_events_user_id_id ON public.events(user_id, id);
CREATE INDEX IF NOT EXISTS idx_events_date ON public.events(date);
CREATE INDEX IF NOT EXISTS idx_events_type ON public.events(type);
CREATE INDEX IF NOT EXISTS idx_events_status ON public.events(status);

CREATE INDEX IF NOT EXISTS idx_merchant_categories_user ON public.merchant_categories(user_id);
CREATE INDEX IF NOT EXISTS idx_merchant_categories_user_id_id ON public.merchant_categories(user_id, id);
CREATE INDEX IF NOT EXISTS idx_merchant_categories_user_merchant ON public.merchant_categories(user_id, merchant);

CREATE INDEX IF NOT EXISTS idx_budgets_user ON public.budgets(user_id);
CREATE INDEX IF NOT EXISTS idx_budgets_user_id_id ON public.budgets(user_id, id);
CREATE INDEX IF NOT EXISTS idx_budgets_category ON public.budgets(category);
CREATE INDEX IF NOT EXISTS idx_budgets_period ON public.budgets(period);

CREATE INDEX IF NOT EXISTS idx_incomes_user ON public.incomes(user_id);
CREATE INDEX IF NOT EXISTS idx_incomes_user_id_id ON public.incomes(user_id, id);
CREATE INDEX IF NOT EXISTS idx_incomes_date ON public.incomes(date);
CREATE INDEX IF NOT EXISTS idx_incomes_source ON public.incomes(source);

CREATE INDEX IF NOT EXISTS idx_recurring_rules_user ON public.recurring_rules(user_id);
CREATE INDEX IF NOT EXISTS idx_recurring_rules_user_id_id ON public.recurring_rules(user_id, id);
CREATE INDEX IF NOT EXISTS idx_recurring_rules_next_run_at ON public.recurring_rules(next_run_at);
CREATE INDEX IF NOT EXISTS idx_recurring_rules_enabled ON public.recurring_rules(enabled);

-- ------------------------------------------------
-- 5) RLS and policies (idempotent)
-- ------------------------------------------------
ALTER TABLE public.transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.merchant_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.budgets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.incomes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.recurring_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users access own transactions" ON public.transactions;
CREATE POLICY "Users access own transactions" ON public.transactions
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users access own tasks" ON public.tasks;
CREATE POLICY "Users access own tasks" ON public.tasks
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users access own events" ON public.events;
CREATE POLICY "Users access own events" ON public.events
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users access own merchant categories" ON public.merchant_categories;
CREATE POLICY "Users access own merchant categories" ON public.merchant_categories
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users access own budgets" ON public.budgets;
CREATE POLICY "Users access own budgets" ON public.budgets
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users access own incomes" ON public.incomes;
CREATE POLICY "Users access own incomes" ON public.incomes
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users access own recurring rules" ON public.recurring_rules;
CREATE POLICY "Users access own recurring rules" ON public.recurring_rules
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users access own profile" ON public.profiles;
CREATE POLICY "Users access own profile" ON public.profiles
    FOR ALL USING (auth.uid() = id) WITH CHECK (auth.uid() = id);

-- ------------------------------------------------
-- 6) Profile bootstrap trigger
-- ------------------------------------------------
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, username, email, member_since)
    VALUES (
        NEW.id,
        NEW.raw_user_meta_data->>'username',
        NEW.email,
        (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
    )
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

COMMIT;

-- Optional verification after run:
-- SELECT conrelid::regclass AS table_name, pg_get_constraintdef(oid) AS pk_def
-- FROM pg_constraint
-- WHERE contype = 'p'
--   AND conrelid::regclass::text IN (
--     'public.transactions','public.tasks','public.events',
--     'public.merchant_categories','public.budgets',
--     'public.incomes','public.recurring_rules'
--   )
-- ORDER BY 1;
