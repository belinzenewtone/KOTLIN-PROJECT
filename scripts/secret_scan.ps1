Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$scanRoots = @(
    "app/src/main",
    "docs",
    "scripts",
    "*.gradle.kts",
    "*.md"
)

$excludedPathFragments = @(
    ".gradle",
    "build",
    ".kotlin",
    "local.properties"
)

$secretPatterns = @(
    "sk-[A-Za-z0-9]{20,}",
    "AIza[0-9A-Za-z\-_]{20,}",
    "BEGIN\s+PRIVATE\s+KEY",
    "xox[baprs]-[A-Za-z0-9\-]{20,}",
    "(?i)aws(.{0,20})?(secret|access).{0,20}['\""]?[A-Za-z0-9\/+=]{20,}"
)

$files = New-Object System.Collections.Generic.List[string]

foreach ($root in $scanRoots) {
    if ($root.Contains("*")) {
        Get-ChildItem -Path . -Filter $root -File | ForEach-Object {
            $files.Add($_.FullName)
        }
        continue
    }

    if (Test-Path $root) {
        Get-ChildItem -Path $root -Recurse -File | ForEach-Object {
            $files.Add($_.FullName)
        }
    }
}

$violations = New-Object System.Collections.Generic.List[string]

foreach ($file in $files | Sort-Object -Unique) {
    $normalized = $file.Replace("\", "/")
    if ($normalized.EndsWith("/scripts/secret_scan.ps1")) {
        continue
    }
    if ($excludedPathFragments | Where-Object { $normalized.Contains($_) }) {
        continue
    }

    $lines = Get-Content -Path $file
    for ($lineNumber = 0; $lineNumber -lt $lines.Count; $lineNumber++) {
        $line = $lines[$lineNumber]
        foreach ($pattern in $secretPatterns) {
            if ($line -match $pattern) {
                $relative = Resolve-Path -Relative $file
                $violations.Add("${relative}:$($lineNumber + 1): potential secret pattern [$pattern]")
                break
            }
        }
    }
}

if ($violations.Count -gt 0) {
    Write-Host "Secret scan failed.`n"
    $violations | Sort-Object | ForEach-Object { Write-Host $_ }
    exit 1
}

Write-Host "Secret scan passed."
