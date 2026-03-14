# Supabase Setup

1) Apply schema in Supabase SQL editor using `supabase/schema.sql`.
If you already created older tables, migrate them so `transactions.source_hash` and owner-scoped indexes/policies exist.

If you get `ERROR: 42703: column "owner_id" does not exist`, run:

1. `supabase/owner_id_compat.sql`
2. then `supabase/schema.sql` again

2) Ensure Email auth provider is enabled in Supabase Auth settings.

3) Run app with Supabase configured through `--dart-define` values:

```bash
flutter run \
  --dart-define=SUPABASE_URL=https://sjapmklwyibqvatssctw.supabase.co \
  --dart-define=SUPABASE_PUBLISHABLE_KEY=YOUR_PUBLISHABLE_OR_ANON_KEY
```

Optional fallback key name:

```bash
--dart-define=SUPABASE_ANON_KEY=YOUR_LEGACY_ANON_KEY
```

Database behavior:

- Every row is scoped by the authenticated user (`owner_id` / profile `id`).
- RLS policies in the schema enforce per-user isolation.
- Same credentials on another phone sign in to the same cloud data.
- SMS imports are deduplicated using `transactions.source_hash`.
- OTA update prompts can be driven by `app_updates` table.

App update row example (insert one active row):

```sql
insert into public.app_updates(
  active,
  latest_version,
  min_supported_version,
  force_update,
  title,
  message,
  notes,
  apk_url,
  website_url
) values (
  true,
  '1.0.2',
  '1.0.0',
  false,
  'Update BELTECH App',
  'A newer update is available. Please update now.',
  array['Performance improvements', 'New features', 'Bug fixes'],
  'https://example.com/app-release.apk',
  'https://example.com/download'
);
```

Assistant proxy hardening:

1) Deploy edge function in `supabase/functions/assistant-proxy`.
2) Set server secrets:
   - `OPENAI_API_KEY`
   - `OPENAI_MODEL` (optional)
3) Use default endpoint:
   - `<SUPABASE_URL>/functions/v1/assistant-proxy`
4) Optionally override app endpoint:

```bash
--dart-define=ASSISTANT_PROXY_URL=https://<project>.supabase.co/functions/v1/assistant-proxy
```

Deployment checklist: `supabase/ROLLOUT_CHECKLIST.md`
