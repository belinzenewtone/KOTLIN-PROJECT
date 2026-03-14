# Supabase Rollout Checklist

## 1. Schema
- [ ] Run `supabase/schema.sql` in SQL editor.
- [ ] Confirm tables have `owner_id` (except `user_profile.id`) and RLS enabled.
- [ ] Confirm `transactions.source_hash` exists.
- [ ] Confirm unique index `idx_transactions_owner_source_hash` exists.

## 2. Auth
- [ ] Enable Email auth provider.
- [ ] Verify sign-up/sign-in works on device.
- [ ] Verify same account on second phone sees same data.

## 3. Edge Function Proxy
- [ ] Deploy `supabase/functions/assistant-proxy`.
- [ ] Set secrets:
  - [ ] `OPENAI_API_KEY`
  - [ ] `OPENAI_MODEL` (optional)
- [ ] Test function endpoint returns `reply` for valid POST.

## 4. App Runtime Defines
- [ ] `SUPABASE_URL`
- [ ] `SUPABASE_PUBLISHABLE_KEY` (or `SUPABASE_ANON_KEY`)
- [ ] `ASSISTANT_PROXY_URL` (optional override)

## 5. Device Permissions
- [ ] Allow SMS permission (for MPESA import/sync).
- [ ] Allow notification permission (task/event reminders).

## 6. Verification
- [ ] `flutter analyze` passes.
- [ ] `flutter test` passes.
- [ ] Add one MPESA SMS, ensure it imports once (no duplicate re-import).
- [ ] Create task/event with reminder, verify notification appears.

## 7. OTA Updates
- [ ] Confirm `public.app_updates` table exists from schema.
- [ ] Insert one active update row with `latest_version`.
- [ ] Set `min_supported_version` and `force_update` as needed.
- [ ] Provide either `apk_url` (Android in-app install) or `website_url`.
- [ ] Launch app and confirm update dialog appears with release notes.
