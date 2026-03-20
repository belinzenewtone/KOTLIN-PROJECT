Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$presentationRoots = @(
    "app/src/main/java/com/personal/lifeOS/features",
    "app/src/main/java/com/personal/lifeOS/ui",
    "app/src/main/java/com/personal/lifeOS/navigation"
)

$bannedPatterns = @(
    "import\s+androidx\.room\.",
    "import\s+okhttp3\.",
    "import\s+retrofit2\.",
    "import\s+com\.personal\.lifeOS\.core\.database\.dao\.",
    "import\s+com\.personal\.lifeOS\.core\.database\.entity\.",
    "import\s+com\.personal\.lifeOS\.core\.utils\.CloudSyncService",
    "import\s+com\.personal\.lifeOS\.core\.utils\.SupabaseClient",
    "import\s+com\.personal\.lifeOS\.features\.auth\.data\.SupabaseAuthClient",
    "import\s+com\.personal\.lifeOS\.features\.assistant\.data\.datasource\.AssistantProxyClient"
)

$violations = New-Object System.Collections.Generic.List[string]

foreach ($root in $presentationRoots) {
    if (-not (Test-Path $root)) {
        continue
    }

    Get-ChildItem -Path $root -Recurse -Filter *.kt -File | ForEach-Object {
        $file = $_.FullName
        $normalized = $file.Replace("\", "/")
        if ($normalized.Contains("/features/") -and -not $normalized.Contains("/presentation/")) {
            return
        }
        $relative = Resolve-Path -Relative $file
        $content = Get-Content -Path $file

        for ($lineNumber = 0; $lineNumber -lt $content.Count; $lineNumber++) {
            $line = $content[$lineNumber]
            foreach ($pattern in $bannedPatterns) {
                if ($line -match $pattern) {
                    $violations.Add("${relative}:$($lineNumber + 1): architecture-boundary violation: $line")
                    break
                }
            }
        }
    }
}

if ($violations.Count -gt 0) {
    Write-Host "Architecture boundary check failed.`n"
    $violations | Sort-Object | ForEach-Object { Write-Host $_ }
    exit 1
}

Write-Host "Architecture boundary check passed."
