param(
    [Parameter(Mandatory = $true)]
    [long]$VersionCode,
    [Parameter(Mandatory = $true)]
    [string]$VersionName,
    [Parameter(Mandatory = $true)]
    [string]$ReleaseNotes,
    [string]$Repo = "",
    [string]$Tag = "",
    [string]$ApkPath = "app/build/outputs/apk/release/app-release.apk",
    [string]$AssetName = "",
    [string]$ManifestPath = "ota/manifest.json",
    [switch]$Required,
    [switch]$BuildRelease,
    [switch]$UpdateLocalProperties
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message"
}

function Resolve-RepoFromOrigin {
    $originUrl = (git remote get-url origin).Trim()
    if ([string]::IsNullOrWhiteSpace($originUrl)) {
        throw "Failed to resolve origin URL."
    }
    if ($originUrl -match "github\.com[:/](?<owner>[^/]+)/(?<name>[^/.]+)(\.git)?$") {
        return "$($Matches.owner)/$($Matches.name)"
    }
    throw "Origin remote is not a GitHub repo URL: $originUrl"
}

function Get-StatusCode {
    param([System.Management.Automation.ErrorRecord]$ErrorRecord)
    $response = $ErrorRecord.Exception.Response
    if ($null -eq $response) { return -1 }
    return [int]$response.StatusCode
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

if ([string]::IsNullOrWhiteSpace($Repo)) {
    $Repo = Resolve-RepoFromOrigin
}
if ([string]::IsNullOrWhiteSpace($Tag)) {
    $Tag = "v$VersionName"
}
if ([string]::IsNullOrWhiteSpace($AssetName)) {
    $AssetName = "app-release-v$VersionName.apk"
}

if ($BuildRelease -or -not (Test-Path $ApkPath)) {
    Write-Step "Building release APK"
    & .\gradlew.bat :app:assembleRelease --no-daemon
}

if (-not (Test-Path $ApkPath)) {
    throw "APK not found at '$ApkPath'. Build the release first or pass -ApkPath."
}

$token = $env:GITHUB_TOKEN
if ([string]::IsNullOrWhiteSpace($token)) {
    $token = $env:GH_TOKEN
}
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "Set GITHUB_TOKEN (or GH_TOKEN) in your environment before running this script."
}

$sha256 = (Get-FileHash -Path $ApkPath -Algorithm SHA256).Hash.ToLowerInvariant()
Write-Step "Computed APK SHA-256: $sha256"

$headers = @{
    Authorization = "Bearer $token"
    Accept = "application/vnd.github+json"
    "X-GitHub-Api-Version" = "2022-11-28"
}

$releaseApiUrl = "https://api.github.com/repos/$Repo/releases/tags/$Tag"

Write-Step "Resolving release '$Tag' in $Repo"
try {
    $release = Invoke-RestMethod -Method Get -Uri $releaseApiUrl -Headers $headers
} catch {
    if ((Get-StatusCode $_) -ne 404) { throw }
    Write-Host "Release not found. Creating a new release..."
    $createBody = @{
        tag_name = $Tag
        name = $Tag
        draft = $false
        prerelease = $false
        body = $ReleaseNotes
    } | ConvertTo-Json -Depth 5

    $release = Invoke-RestMethod `
        -Method Post `
        -Uri "https://api.github.com/repos/$Repo/releases" `
        -Headers $headers `
        -Body $createBody `
        -ContentType "application/json"
}

if ($release.assets) {
    $existingAsset = $release.assets | Where-Object { $_.name -eq $AssetName } | Select-Object -First 1
    if ($existingAsset) {
        Write-Step "Deleting existing asset '$AssetName'"
        Invoke-RestMethod `
            -Method Delete `
            -Uri "https://api.github.com/repos/$Repo/releases/assets/$($existingAsset.id)" `
            -Headers $headers
    }
}

$uploadUrlBase = $release.upload_url -replace "\{\?name,label\}", ""
$assetUploadUrl = "$uploadUrlBase?name=$([uri]::EscapeDataString($AssetName))"

Write-Step "Uploading APK asset '$AssetName'"
$uploadedAsset = Invoke-RestMethod `
    -Method Post `
    -Uri $assetUploadUrl `
    -Headers $headers `
    -InFile $ApkPath `
    -ContentType "application/vnd.android.package-archive"

$downloadUrl = $uploadedAsset.browser_download_url
if ([string]::IsNullOrWhiteSpace($downloadUrl)) {
    throw "GitHub API did not return a browser_download_url for the uploaded asset."
}

$manifestDir = Split-Path -Parent $ManifestPath
if (-not [string]::IsNullOrWhiteSpace($manifestDir) -and -not (Test-Path $manifestDir)) {
    New-Item -ItemType Directory -Path $manifestDir | Out-Null
}

$manifest = [ordered]@{
    version_code = $VersionCode
    version_name = $VersionName
    download_url = $downloadUrl
    checksum_sha256 = $sha256
    required = [bool]$Required.IsPresent
    release_notes = $ReleaseNotes
}

Write-Step "Writing OTA manifest to '$ManifestPath'"
$manifest | ConvertTo-Json -Depth 5 | Set-Content -Path $ManifestPath -Encoding UTF8

if ($UpdateLocalProperties) {
    $manifestUrl = "https://raw.githubusercontent.com/$Repo/main/ota/manifest.json"
    $localPropertiesPath = "local.properties"
    if (Test-Path $localPropertiesPath) {
        $lines = Get-Content $localPropertiesPath
        $updated = $false
        $newLines = foreach ($line in $lines) {
            if ($line -match "^OTA_MANIFEST_URL=") {
                $updated = $true
                "OTA_MANIFEST_URL=$manifestUrl"
            } else {
                $line
            }
        }
        if (-not $updated) {
            $newLines += "OTA_MANIFEST_URL=$manifestUrl"
        }
        $newLines | Set-Content -Path $localPropertiesPath -Encoding UTF8
    } else {
        "OTA_MANIFEST_URL=$manifestUrl" | Set-Content -Path $localPropertiesPath -Encoding UTF8
    }
    Write-Step "Updated local.properties OTA_MANIFEST_URL"
}

Write-Step "Done"
Write-Host "Release URL: $($release.html_url)"
Write-Host "Asset URL:   $downloadUrl"
Write-Host "Manifest:    $ManifestPath"
Write-Host ""
Write-Host "Next steps:"
Write-Host "  git add $ManifestPath"
Write-Host "  git commit -m \"chore: update OTA manifest for $Tag\""
Write-Host "  git push origin main"
