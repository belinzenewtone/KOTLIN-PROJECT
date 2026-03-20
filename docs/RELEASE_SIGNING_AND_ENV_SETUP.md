# Release Signing and Runtime Config Setup

This project can build unsigned release artifacts without local secrets, but production release should enforce signing and runtime backend configuration.

## 1) Configure `local.properties`

Add these entries:

```properties
RELEASE_STORE_FILE=/absolute/path/to/your-release.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...

SUPABASE_URL=...
SUPABASE_ANON_KEY=...
ASSISTANT_PROXY_URL=...
OTA_MANIFEST_URL=...
```

For GitHub-hosted OTA, set:

```properties
OTA_MANIFEST_URL=https://raw.githubusercontent.com/belinzenewtone/KOTLIN-PROJECT/main/ota/manifest.json
```

To publish a GitHub release asset and refresh `ota/manifest.json`:

```powershell
$env:GITHUB_TOKEN="<token>"
.\scripts\publish_github_ota_release.ps1 -VersionCode 12 -VersionName 1.2.0 -ReleaseNotes "Stability improvements" -BuildRelease
```

## 2) Run strict release checks

Use Gradle properties to force release-gate checks:

```powershell
./gradlew :app:assembleRelease :app:bundleRelease -PrequireReleaseSigning=true -PrequireReleaseRuntimeConfig=true
```

Optional explicit checks:

```powershell
./gradlew :app:verifyReleaseSigningConfig :app:verifyReleaseRuntimeConfig
```

## 3) Validate signing outcome

```powershell
./gradlew :app:signingReport
```

Expected for production: `Variant: release` should not show `Config: null`.

## 4) Device QA execution

Attach at least one physical device or emulator, then run:

```powershell
./gradlew :app:connectedDebugAndroidTest
```

For release candidate confidence, execute manual smoke flows:
- login/session restore
- onboarding/profile completion
- tasks create/complete/edit
- calendar create/edit/complete
- finance add/import/review
- assistant action preview and commit
- export
- biometric lock/relock
- update prompt surface
