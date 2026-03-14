# assistant-proxy edge function

## Purpose
Server-side proxy for BELTECH assistant replies so the OpenAI API key stays in Supabase secrets, not in the mobile app.

## Required secrets

```bash
supabase secrets set OPENAI_API_KEY=YOUR_KEY
supabase secrets set OPENAI_MODEL=gpt-4.1-mini
```

## Deploy

```bash
supabase functions deploy assistant-proxy
```

## App usage

The Flutter app calls:

`<SUPABASE_URL>/functions/v1/assistant-proxy`

You can override using:

`--dart-define=ASSISTANT_PROXY_URL=https://.../assistant-proxy`
