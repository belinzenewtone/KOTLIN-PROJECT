-- ================================================
-- BELTECH App — Supabase Schema v6
-- Run this in Supabase Dashboard → SQL Editor
-- ================================================

-- Transactions table (MPESA + manual expenses)
CREATE TABLE IF NOT EXISTS transactions (
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
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    PRIMARY KEY (user_id, id)
);

-- Tasks table
CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT DEFAULT '',
    priority TEXT DEFAULT 'MEDIUM',
    deadline BIGINT,
    status TEXT DEFAULT 'PENDING',
    completed_at BIGINT,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    PRIMARY KEY (user_id, id)
);

-- Events table (calendar)
CREATE TABLE IF NOT EXISTS events (
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
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    PRIMARY KEY (user_id, id)
);

-- Merchant categories (learning database)
CREATE TABLE IF NOT EXISTS merchant_categories (
    id BIGINT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    merchant TEXT NOT NULL,
    category TEXT NOT NULL,
    confidence REAL DEFAULT 1.0,
    user_corrected BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (user_id, id),
    UNIQUE(user_id, merchant)
);

-- Budgets table
CREATE TABLE IF NOT EXISTS budgets (
    id BIGINT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    category TEXT NOT NULL,
    limit_amount DOUBLE PRECISION NOT NULL,
    period TEXT NOT NULL DEFAULT 'MONTHLY',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    PRIMARY KEY (user_id, id)
);

-- Incomes table
CREATE TABLE IF NOT EXISTS incomes (
    id BIGINT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    amount DOUBLE PRECISION NOT NULL,
    source TEXT NOT NULL,
    date BIGINT NOT NULL,
    note TEXT DEFAULT '',
    is_recurring BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (user_id, id)
);

-- Recurring rules table
CREATE TABLE IF NOT EXISTS recurring_rules (
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

-- User profiles (extends Supabase auth.users)
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username TEXT,
    full_name TEXT,
    email TEXT,
    phone TEXT,
    profile_pic_url TEXT,
    member_since BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_transactions_user ON transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_user_id_id ON transactions(user_id, id);
CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(date);
CREATE INDEX IF NOT EXISTS idx_transactions_category ON transactions(category);
CREATE INDEX IF NOT EXISTS idx_tasks_user ON tasks(user_id);
CREATE INDEX IF NOT EXISTS idx_tasks_user_id_id ON tasks(user_id, id);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_events_user ON events(user_id);
CREATE INDEX IF NOT EXISTS idx_events_user_id_id ON events(user_id, id);
CREATE INDEX IF NOT EXISTS idx_events_date ON events(date);
CREATE INDEX IF NOT EXISTS idx_events_status ON events(status);
CREATE INDEX IF NOT EXISTS idx_budgets_user ON budgets(user_id);
CREATE INDEX IF NOT EXISTS idx_budgets_user_id_id ON budgets(user_id, id);
CREATE INDEX IF NOT EXISTS idx_budgets_category ON budgets(category);
CREATE INDEX IF NOT EXISTS idx_incomes_user ON incomes(user_id);
CREATE INDEX IF NOT EXISTS idx_incomes_user_id_id ON incomes(user_id, id);
CREATE INDEX IF NOT EXISTS idx_incomes_date ON incomes(date);
CREATE INDEX IF NOT EXISTS idx_recurring_rules_user ON recurring_rules(user_id);
CREATE INDEX IF NOT EXISTS idx_recurring_rules_user_id_id ON recurring_rules(user_id, id);
CREATE INDEX IF NOT EXISTS idx_recurring_rules_next_run_at ON recurring_rules(next_run_at);

-- Enable Row Level Security
ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE events ENABLE ROW LEVEL SECURITY;
ALTER TABLE merchant_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE budgets ENABLE ROW LEVEL SECURITY;
ALTER TABLE incomes ENABLE ROW LEVEL SECURITY;
ALTER TABLE recurring_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

-- RLS Policies: users can only access their own data
CREATE POLICY "Users access own transactions" ON transactions
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users access own tasks" ON tasks
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users access own events" ON events
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users access own merchant categories" ON merchant_categories
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users access own budgets" ON budgets
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users access own incomes" ON incomes
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users access own recurring rules" ON recurring_rules
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users access own profile" ON profiles
    FOR ALL USING (auth.uid() = id) WITH CHECK (auth.uid() = id);

-- Auto-create profile on user signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, username, email, member_since)
    VALUES (
        NEW.id,
        NEW.raw_user_meta_data->>'username',
        NEW.email,
        (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger to auto-create profile
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ================================================
-- Existing v2 project migration notes
-- ================================================
-- If your current tables were created with PRIMARY KEY (id),
-- add a composite unique key to support user-scoped upserts:
--
-- ALTER TABLE transactions ADD CONSTRAINT transactions_user_id_id_key UNIQUE (user_id, id);
-- ALTER TABLE tasks ADD CONSTRAINT tasks_user_id_id_key UNIQUE (user_id, id);
-- ALTER TABLE events ADD CONSTRAINT events_user_id_id_key UNIQUE (user_id, id);
-- ALTER TABLE merchant_categories ADD CONSTRAINT merchant_categories_user_id_id_key UNIQUE (user_id, id);
-- ALTER TABLE budgets ADD CONSTRAINT budgets_user_id_id_key UNIQUE (user_id, id);
-- ALTER TABLE incomes ADD CONSTRAINT incomes_user_id_id_key UNIQUE (user_id, id);
-- ALTER TABLE recurring_rules ADD CONSTRAINT recurring_rules_user_id_id_key UNIQUE (user_id, id);
--
-- and ensure the events table has:
-- ALTER TABLE events ADD COLUMN IF NOT EXISTS importance TEXT DEFAULT 'NEUTRAL';
