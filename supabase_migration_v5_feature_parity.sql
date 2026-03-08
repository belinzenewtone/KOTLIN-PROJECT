-- ================================================
-- BELTECH App — Supabase Migration v5
-- Purpose: add Budget / Income / Recurring sync tables with RLS
-- Date: 2026-03-08
-- ================================================

BEGIN;

CREATE TABLE IF NOT EXISTS public.budgets (
    id BIGINT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    category TEXT NOT NULL,
    limit_amount DOUBLE PRECISION NOT NULL,
    period TEXT NOT NULL DEFAULT 'MONTHLY',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    PRIMARY KEY (user_id, id)
);

CREATE TABLE IF NOT EXISTS public.incomes (
    id BIGINT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    amount DOUBLE PRECISION NOT NULL,
    source TEXT NOT NULL,
    date BIGINT NOT NULL,
    note TEXT DEFAULT '',
    is_recurring BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (user_id, id)
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
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    PRIMARY KEY (user_id, id)
);

CREATE INDEX IF NOT EXISTS idx_budgets_user ON public.budgets(user_id);
CREATE INDEX IF NOT EXISTS idx_budgets_user_id_id ON public.budgets(user_id, id);
CREATE INDEX IF NOT EXISTS idx_budgets_category ON public.budgets(category);

CREATE INDEX IF NOT EXISTS idx_incomes_user ON public.incomes(user_id);
CREATE INDEX IF NOT EXISTS idx_incomes_user_id_id ON public.incomes(user_id, id);
CREATE INDEX IF NOT EXISTS idx_incomes_date ON public.incomes(date);

CREATE INDEX IF NOT EXISTS idx_recurring_rules_user ON public.recurring_rules(user_id);
CREATE INDEX IF NOT EXISTS idx_recurring_rules_user_id_id ON public.recurring_rules(user_id, id);
CREATE INDEX IF NOT EXISTS idx_recurring_rules_next_run_at ON public.recurring_rules(next_run_at);

ALTER TABLE public.budgets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.incomes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.recurring_rules ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users access own budgets" ON public.budgets;
CREATE POLICY "Users access own budgets" ON public.budgets
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users access own incomes" ON public.incomes;
CREATE POLICY "Users access own incomes" ON public.incomes
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users access own recurring rules" ON public.recurring_rules;
CREATE POLICY "Users access own recurring rules" ON public.recurring_rules
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

COMMIT;
