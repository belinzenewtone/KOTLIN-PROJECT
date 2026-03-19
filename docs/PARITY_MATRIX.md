# Kotlin vs RN Parity Matrix

| Area | RN Reference | Kotlin Current | Status |
|---|---|---|---|
| Bootstrap orchestration | Present | Added coordinator + startup model | In progress |
| Deterministic sync queue | Evolving | Added queue + coordinator + dispatcher | In progress |
| Canonical metadata (`syncState/source/revision`) | Partial | Added in Room entities + migration | In progress |
| SMS import isolation | Strong | Added `platform/sms` ingestion path | In progress |
| Assistant proposal/commit safety | Required | Added proposal models + executor contract | In progress |
| Insight/review persistence | Present | Added Room entities + DAOs | In progress |
| Update diagnostics persistence | Present | Added update diagnostics repository + table | In progress |
| Stitch design tokens | Required | Applied base color/typography/spacing refresh + core DS components | In progress |
| Remote config / feature flags | Required | Added local flag store + remote fetch + bootstrap refresh | In progress |
| Architecture quality gates | Required | Added boundary + secret scans to Gradle `check` | In progress |
| Diagnostics health surface | Required | Added sync/import/update diagnostics panel in Settings | In progress |
| Deterministic insight engine | Required | Added local rules engine + Room cache + dashboard integration | In progress |
| Assistant mutation safety | Required | Added proposal parsing + explicit confirm/cancel + commit handlers for task/expense | In progress |
| Assistant local history | Required | Assistant conversations/messages now loaded and persisted through Room DAOs | In progress |
| Sync queue idempotency | Required | Added active-job dedupe + mutation enqueue hooks in mutable repositories | In progress |
| SMS subsystem isolation | Required | Removed legacy feature-local SMS receiver/worker; platform runtime path only | In progress |
| M-Pesa dedupe heuristics | Required | Added amount+merchant+timestamp duplicate check fallback (with tests) | In progress |
