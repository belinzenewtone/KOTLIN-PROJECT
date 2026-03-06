-- BELTECH App — Supabase Schema
-- Run this in your Supabase SQL Editor (Dashboard → SQL Editor → New Query)

-- Transactions table (MPESA + manual expenses)
CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT PRIMARY KEY,
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

-- Tasks table
CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT DEFAULT '',
    priority TEXT DEFAULT 'MEDIUM',
    deadline BIGINT,
    status TEXT DEFAULT 'PENDING',
    completed_at BIGINT,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

-- Events table (calendar)
CREATE TABLE IF NOT EXISTS events (
    id BIGINT PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT DEFAULT '',
    date BIGINT NOT NULL,
    end_date BIGINT,
    type TEXT DEFAULT 'PERSONAL',
    has_reminder BOOLEAN DEFAULT FALSE,
    reminder_minutes_before INTEGER DEFAULT 15,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

-- Merchant categories (learning database)
CREATE TABLE IF NOT EXISTS merchant_categories (
    id BIGINT PRIMARY KEY,
    merchant TEXT NOT NULL UNIQUE,
    category TEXT NOT NULL,
    confidence REAL DEFAULT 1.0,
    "userCorrected" BOOLEAN DEFAULT FALSE
);

-- Enable Row Level Security (optional but recommended)
ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE events ENABLE ROW LEVEL SECURITY;
ALTER TABLE merchant_categories ENABLE ROW LEVEL SECURITY;

-- Allow all operations with anon key (simple setup)
-- For production, add proper auth policies
CREATE POLICY "Allow all" ON transactions FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow all" ON tasks FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow all" ON events FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow all" ON merchant_categories FOR ALL USING (true) WITH CHECK (true);
