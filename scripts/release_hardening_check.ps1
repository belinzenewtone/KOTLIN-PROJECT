param(
    [switch]$SkipStaticChecks
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message"
}

function Invoke-Step {
    param([string]$Command)
    Write-Host "Running: $Command"
    Invoke-Expression $Command
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$jbrPath = "C:\Program Files\Android\Android Studio\jbr"
if (Test-Path $jbrPath) {
    $env:JAVA_HOME = $jbrPath
    $env:PATH = "$($env:JAVA_HOME)\bin;$($env:PATH)"
}
$env:GRADLE_OPTS = "-Xmx1024m"

Write-Step "Unit tests"
Invoke-Step ".\gradlew.bat testDebugUnitTest --no-daemon"

if (-not $SkipStaticChecks) {
    Write-Step "Static checks (blocking; baseline-backed)"
    Invoke-Step ".\gradlew.bat app:detekt app:ktlintCheck --no-daemon"
}

Write-Step "Supabase strict smoke test (if credentials are provided)"
if (
    [string]::IsNullOrWhiteSpace($env:SUPABASE_PRIMARY_EMAIL) -or
    [string]::IsNullOrWhiteSpace($env:SUPABASE_PRIMARY_PASSWORD) -or
    [string]::IsNullOrWhiteSpace($env:SUPABASE_SECONDARY_EMAIL) -or
    [string]::IsNullOrWhiteSpace($env:SUPABASE_SECONDARY_PASSWORD)
) {
    Write-Warning "Skipping strict smoke test. Set SUPABASE_PRIMARY/SECONDARY credentials env vars first."
    exit 0
}

$props = @{}
Get-Content ".\local.properties" | Where-Object { $_ -match "=" } | ForEach-Object {
    $k, $v = $_.Split("=", 2)
    $props[$k] = $v
}

$supabaseUrl = $props["SUPABASE_URL"] -replace "\\:", ":"
$anonKey = $props["SUPABASE_ANON_KEY"]

if ([string]::IsNullOrWhiteSpace($supabaseUrl) -or [string]::IsNullOrWhiteSpace($anonKey)) {
    throw "SUPABASE_URL or SUPABASE_ANON_KEY missing in local.properties"
}

Invoke-Step ".\scripts\supabase_two_user_smoke_test.ps1 -SupabaseUrl '$supabaseUrl' -AnonKey '$anonKey' -RequireTwoUsers"
