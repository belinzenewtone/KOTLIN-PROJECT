-- ================================================
-- BELTECH App — Supabase Migration v9
-- Purpose: tighten role grants while keeping RLS-user access functional
-- Date: 2026-03-20
-- ================================================

BEGIN;

REVOKE ALL ON TABLE public.transactions FROM anon;
REVOKE ALL ON TABLE public.tasks FROM anon;
REVOKE ALL ON TABLE public.events FROM anon;
REVOKE ALL ON TABLE public.merchant_categories FROM anon;
REVOKE ALL ON TABLE public.budgets FROM anon;
REVOKE ALL ON TABLE public.incomes FROM anon;
REVOKE ALL ON TABLE public.recurring_rules FROM anon;
REVOKE ALL ON TABLE public.profiles FROM anon;
REVOKE ALL ON TABLE public.sync_jobs FROM anon;
REVOKE ALL ON TABLE public.import_audit FROM anon;
REVOKE ALL ON TABLE public.assistant_conversations FROM anon;
REVOKE ALL ON TABLE public.assistant_messages FROM anon;
REVOKE ALL ON TABLE public.insight_cards FROM anon;
REVOKE ALL ON TABLE public.review_snapshots FROM anon;
REVOKE ALL ON TABLE public.app_update_info FROM anon;

REVOKE ALL ON TABLE public.transactions FROM authenticated;
REVOKE ALL ON TABLE public.tasks FROM authenticated;
REVOKE ALL ON TABLE public.events FROM authenticated;
REVOKE ALL ON TABLE public.merchant_categories FROM authenticated;
REVOKE ALL ON TABLE public.budgets FROM authenticated;
REVOKE ALL ON TABLE public.incomes FROM authenticated;
REVOKE ALL ON TABLE public.recurring_rules FROM authenticated;
REVOKE ALL ON TABLE public.profiles FROM authenticated;
REVOKE ALL ON TABLE public.sync_jobs FROM authenticated;
REVOKE ALL ON TABLE public.import_audit FROM authenticated;
REVOKE ALL ON TABLE public.assistant_conversations FROM authenticated;
REVOKE ALL ON TABLE public.assistant_messages FROM authenticated;
REVOKE ALL ON TABLE public.insight_cards FROM authenticated;
REVOKE ALL ON TABLE public.review_snapshots FROM authenticated;
REVOKE ALL ON TABLE public.app_update_info FROM authenticated;

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.transactions TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.tasks TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.events TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.merchant_categories TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.budgets TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.incomes TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.recurring_rules TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.profiles TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.sync_jobs TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.import_audit TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.assistant_conversations TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.assistant_messages TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.insight_cards TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.review_snapshots TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.app_update_info TO authenticated;

REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM anon;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM authenticated;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO authenticated;

COMMIT;
