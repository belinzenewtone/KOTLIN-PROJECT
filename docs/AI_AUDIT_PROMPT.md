# Copy-Paste Prompt For A New AI Auditor

You are auditing and hardening an Android Kotlin project (`Personal OS`) that has already undergone a major revamp.

## Context

- Repo: `https://github.com/belinzenewtone/DART-2.0`
- Branch: `codex/full-revamp-android`
- Target commit to start from: `5a943dc`
- Primary handoff document: `docs/AI_AUDIT_HANDOFF.md`
- Canonical engineering rules: `CODING_RULES.md`

## Objectives

1. Audit the current implementation for anything missed in architecture, correctness, testing, security, and UX consistency.
2. Fix high-confidence issues directly in code.
3. Add/adjust tests for every critical logic-path change.
4. Keep the app aligned with strict architecture rules:
   - `UI -> ViewModel -> UseCase -> Repository -> Local/Network/Platform`
   - Room local-first source of truth
   - explicit sync safety
   - assistant proposal/preview safety
   - no secrets in app source

## UI/UX Direction

- Stitch-first reference with function-first adaptation:
  - `docs/DESIGN_REFERENCE_PACK.md`
- Keep Android-native performance and readability priorities.
- Ensure loading/empty/error/offline/sync-aware states are explicit on major screens.

## Backend/Supabase Expectations

- Supabase migrations through v9 are expected.
- RLS must remain enabled with user-scoped policies.
- Edge functions expected:
  - `assistant-proxy`
  - `ota-manifest`

## Mandatory Verification

Run and report:

```powershell
.\gradlew.bat :app:check
.\gradlew.bat assembleDebug
```

If touching release logic:

```powershell
.\gradlew.bat :app:assembleRelease :app:bundleRelease :app:lintRelease
```

## Deliverables Format

1. Findings first (ordered by severity), with precise file references.
2. Fixes applied.
3. Tests added/updated.
4. Residual risks and what still needs manual verification.
5. Updated docs if architecture decisions changed.
