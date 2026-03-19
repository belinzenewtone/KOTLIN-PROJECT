param(
    [string]$SupabaseUrl = $env:SUPABASE_URL,
    [string]$AnonKey = $env:SUPABASE_ANON_KEY,
    [string]$PrimaryEmail = $env:SUPABASE_PRIMARY_EMAIL,
    [string]$PrimaryPassword = $env:SUPABASE_PRIMARY_PASSWORD,
    [string]$SecondaryEmail = $env:SUPABASE_SECONDARY_EMAIL,
    [string]$SecondaryPassword = $env:SUPABASE_SECONDARY_PASSWORD,
    [switch]$RequireTwoUsers
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Parse-JsonOrNull {
    param([string]$Content)
    if ([string]::IsNullOrWhiteSpace($Content)) { return $null }
    try { return $Content | ConvertFrom-Json -Depth 100 }
    catch { return $null }
}

function Invoke-SupabaseRequest {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [string]$AccessToken = "",
        [hashtable]$ExtraHeaders = @{}
    )

    $headers = @{
        "apikey" = $AnonKey
    }
    if (-not [string]::IsNullOrWhiteSpace($AccessToken)) {
        $headers["Authorization"] = "Bearer $AccessToken"
    }
    foreach ($key in $ExtraHeaders.Keys) {
        $headers[$key] = $ExtraHeaders[$key]
    }

    $uri = "$SupabaseUrl$Path"
    $bodyJson = $null
    if ($null -ne $Body) {
        $bodyJson = $Body | ConvertTo-Json -Depth 20 -Compress
    }

    try {
        $response = Invoke-WebRequest `
            -Uri $uri `
            -Method $Method `
            -Headers $headers `
            -Body $bodyJson `
            -ContentType "application/json" `
            -ErrorAction Stop
        $content = [string]$response.Content
        return [pscustomobject]@{
            ok = $true
            status = [int]$response.StatusCode
            content = $content
            json = Parse-JsonOrNull $content
        }
    } catch {
        $status = $null
        $content = ""
        $httpResponse = $_.Exception.Response
        if ($null -ne $httpResponse) {
            if ($httpResponse -is [System.Net.Http.HttpResponseMessage]) {
                try { $status = [int]$httpResponse.StatusCode } catch {}
                try { $content = $httpResponse.Content.ReadAsStringAsync().Result } catch {}
            } else {
                try { $status = [int]$httpResponse.StatusCode } catch {}
                try {
                    $stream = $httpResponse.GetResponseStream()
                    if ($null -ne $stream) {
                        $reader = New-Object System.IO.StreamReader($stream)
                        $content = $reader.ReadToEnd()
                    }
                } catch {}
            }
        }
        if ([string]::IsNullOrWhiteSpace($content) -and $null -ne $_.ErrorDetails -and -not [string]::IsNullOrWhiteSpace($_.ErrorDetails.Message)) {
            $content = [string]$_.ErrorDetails.Message
        }
        if ($null -eq $status) { $status = 0 }
        return [pscustomobject]@{
            ok = $false
            status = $status
            content = $content
            json = Parse-JsonOrNull $content
        }
    }
}

function Get-Session {
    param(
        [string]$Email,
        [string]$Password
    )
    $resp = Invoke-SupabaseRequest `
        -Method "POST" `
        -Path "/auth/v1/token?grant_type=password" `
        -Body @{
            email = $Email
            password = $Password
        }
    if (-not $resp.ok) {
        throw "Sign-in failed for $Email (HTTP $($resp.status)): $($resp.content)"
    }
    if ($null -eq $resp.json.access_token -or $null -eq $resp.json.user.id) {
        throw "Sign-in response missing token/user for ${Email}: $($resp.content)"
    }
    return [pscustomobject]@{
        email = $Email
        token = [string]$resp.json.access_token
        userId = [string]$resp.json.user.id
    }
}

function Try-CreateSecondarySession {
    param(
        [string]$Email,
        [string]$Password
    )

    if ([string]::IsNullOrWhiteSpace($Email)) {
        $Email = "codex.two.user.$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())@gmail.com"
    }
    if ([string]::IsNullOrWhiteSpace($Password)) {
        $Password = "Codex!$([Guid]::NewGuid().ToString('N').Substring(0, 12))"
    }

    $signup = Invoke-SupabaseRequest `
        -Method "POST" `
        -Path "/auth/v1/signup" `
        -Body @{
            email = $Email
            password = $Password
        }

    if (-not $signup.ok) {
        return [pscustomobject]@{
            ok = $false
            reason = "signup_failed"
            email = $Email
            password = $Password
            detail = "HTTP $($signup.status): $($signup.content)"
        }
    }

    if ($null -ne $signup.json.access_token -and $null -ne $signup.json.user.id) {
        return [pscustomobject]@{
            ok = $true
            session = [pscustomobject]@{
                email = $Email
                token = [string]$signup.json.access_token
                userId = [string]$signup.json.user.id
            }
            created = $true
        }
    }

    # Email confirmation required path.
    $signin = Invoke-SupabaseRequest `
        -Method "POST" `
        -Path "/auth/v1/token?grant_type=password" `
        -Body @{
            email = $Email
            password = $Password
        }
    if ($signin.ok -and $null -ne $signin.json.access_token -and $null -ne $signin.json.user.id) {
        return [pscustomobject]@{
            ok = $true
            session = [pscustomobject]@{
                email = $Email
                token = [string]$signin.json.access_token
                userId = [string]$signin.json.user.id
            }
            created = $true
        }
    }

    return [pscustomobject]@{
        ok = $false
        reason = "confirmation_or_signin_failed"
        email = $Email
        password = $Password
        detail = "Signup did not return a session and sign-in failed: HTTP $($signin.status) $($signin.content)"
    }
}

function To-Array {
    param([object]$Value)
    if ($null -eq $Value) { return @() }
    if ($Value -is [System.Array]) { return $Value }
    return @($Value)
}

if ([string]::IsNullOrWhiteSpace($SupabaseUrl) -or [string]::IsNullOrWhiteSpace($AnonKey)) {
    throw "Missing SUPABASE_URL or SUPABASE_ANON_KEY."
}
if ([string]::IsNullOrWhiteSpace($PrimaryEmail) -or [string]::IsNullOrWhiteSpace($PrimaryPassword)) {
    throw "Missing primary credentials. Set SUPABASE_PRIMARY_EMAIL and SUPABASE_PRIMARY_PASSWORD."
}

Write-Host "Supabase two-user smoke test started."

$primary = Get-Session -Email $PrimaryEmail -Password $PrimaryPassword
Write-Host ("Primary session ok: {0}" -f $primary.userId)

$secondary = $null
$secondaryOrigin = ""
if (-not [string]::IsNullOrWhiteSpace($SecondaryEmail) -and -not [string]::IsNullOrWhiteSpace($SecondaryPassword)) {
    $secondary = Get-Session -Email $SecondaryEmail -Password $SecondaryPassword
    $secondaryOrigin = "provided"
} else {
    $created = Try-CreateSecondarySession -Email $SecondaryEmail -Password $SecondaryPassword
    if ($created.ok) {
        $secondary = $created.session
        $secondaryOrigin = "generated"
        Write-Host "Secondary account auto-created and authenticated."
    } else {
        Write-Warning "Could not create/authenticate secondary account."
        Write-Warning $created.detail
    }
}

$runSuffix = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )
    if (-not $Condition) { throw $Message }
}

if ($null -eq $secondary) {
    Write-Host "Falling back to single-user RLS check (secondary account unavailable)."

    $wrongUserId = [Guid]::NewGuid().ToString()
    $txId = [int64](830000000000 + ($runSuffix % 1000000))
    $forbiddenInsert = Invoke-SupabaseRequest `
        -Method "POST" `
        -Path "/rest/v1/transactions?on_conflict=user_id,id" `
        -AccessToken $primary.token `
        -ExtraHeaders @{
            "Prefer" = "resolution=merge-duplicates,return=representation"
        } `
        -Body @(
            @{
                id = $txId
                user_id = $wrongUserId
                amount = 1.23
                merchant = "forbidden-check"
                category = "TEST"
                date = $runSuffix
                source = "MANUAL"
                transaction_type = "SENT"
            }
        )

    Assert-True -Condition (-not $forbiddenInsert.ok) -Message "RLS check failed: wrong-user insert unexpectedly succeeded."
    Assert-True -Condition ($forbiddenInsert.status -in @(401, 403)) -Message "Expected HTTP 401/403 on wrong-user insert, got $($forbiddenInsert.status)."
    Write-Host "Single-user fallback passed: wrong-user insert was blocked by RLS."
    if ($RequireTwoUsers) {
        exit 2
    }
    Write-Host "Result: PARTIAL_PASS (two-user path skipped)"
    exit 0
}

Write-Host ("Secondary session ok ({0}): {1}" -f $secondaryOrigin, $secondary.userId)

$tableTests = @(
    @{
        table = "transactions"
        id = [int64](910000000000 + ($runSuffix % 1000000))
        makeA = {
            param($id, $userId)
            return @{
                id = $id
                user_id = $userId
                amount = 11.11
                merchant = "a-merchant-$runSuffix"
                category = "A_CATEGORY"
                date = $runSuffix
                source = "MANUAL"
                transaction_type = "SENT"
            }
        }
        makeB = {
            param($id, $userId)
            return @{
                id = $id
                user_id = $userId
                amount = 22.22
                merchant = "b-merchant-$runSuffix"
                category = "B_CATEGORY"
                date = $runSuffix + 1
                source = "MPESA"
                transaction_type = "RECEIVED"
            }
        }
        marker = "merchant"
    }
    @{
        table = "tasks"
        id = [int64](920000000000 + ($runSuffix % 1000000))
        makeA = {
            param($id, $userId)
            return @{
                id = $id
                user_id = $userId
                title = "Task-A-$runSuffix"
                description = "A-desc"
                priority = "HIGH"
                status = "PENDING"
            }
        }
        makeB = {
            param($id, $userId)
            return @{
                id = $id
                user_id = $userId
                title = "Task-B-$runSuffix"
                description = "B-desc"
                priority = "LOW"
                status = "COMPLETED"
            }
        }
        marker = "title"
    }
    @{
        table = "events"
        id = [int64](930000000000 + ($runSuffix % 1000000))
        makeA = {
            param($id, $userId)
            return @{
                id = $id
                user_id = $userId
                title = "Event-A-$runSuffix"
                description = "A-event"
                date = $runSuffix + 5000
                end_date = $runSuffix + 8000
                type = "PERSONAL"
                importance = "IMPORTANT"
                status = "PENDING"
                has_reminder = $true
                reminder_minutes_before = 10
            }
        }
        makeB = {
            param($id, $userId)
            return @{
                id = $id
                user_id = $userId
                title = "Event-B-$runSuffix"
                description = "B-event"
                date = $runSuffix + 9000
                end_date = $runSuffix + 12000
                type = "WORK"
                importance = "NEUTRAL"
                status = "COMPLETED"
                has_reminder = $false
                reminder_minutes_before = 15
            }
        }
        marker = "title"
    }
    @{
        table = "merchant_categories"
        id = [int64](940000000000 + ($runSuffix % 1000000))
        makeA = {
            param($id, $userId)
            return @{
                id = $id
                user_id = $userId
                merchant = "mc-a-$runSuffix"
                category = "FOOD"
                confidence = 0.8
                user_corrected = $true
            }
        }
        makeB = {
            param($id, $userId)
            return @{
                id = $id
                user_id = $userId
                merchant = "mc-b-$runSuffix"
                category = "TRANSPORT"
                confidence = 0.9
                user_corrected = $false
            }
        }
        marker = "category"
    }
    @{
        table = "budgets"
        id = [int64](950000000000 + ($runSuffix % 1000000))
        makeA = {
            param($id, $userId)
            return @{
                id = $id
                user_id = $userId
                category = "FOOD"
                limit_amount = 1000.0
                period = "MONTHLY"
                created_at = $runSuffix
            }
        }
        makeB = {
            param($id, $userId)
            return @{
                id = $id
                user_id = $userId
                category = "TRANSPORT"
                limit_amount = 2000.0
                period = "WEEKLY"
                created_at = $runSuffix + 1
            }
        }
        marker = "category"
    }
    @{
        table = "incomes"
        id = [int64](960000000000 + ($runSuffix % 1000000))
        makeA = {
            param($id, $userId)
            return @{
                id = $id
                user_id = $userId
                amount = 5555.0
                source = "Salary-A"
                date = $runSuffix
                note = "A note"
                is_recurring = $true
            }
        }
        makeB = {
            param($id, $userId)
            return @{
                id = $id
                user_id = $userId
                amount = 7777.0
                source = "Salary-B"
                date = $runSuffix + 2
                note = "B note"
                is_recurring = $false
            }
        }
        marker = "source"
    }
    @{
        table = "recurring_rules"
        id = [int64](970000000000 + ($runSuffix % 1000000))
        makeA = {
            param($id, $userId)
            return @{
                id = $id
                user_id = $userId
                title = "Rule-A-$runSuffix"
                type = "EXPENSE"
                cadence = "MONTHLY"
                next_run_at = $runSuffix + 100000
                amount = 123.0
                enabled = $true
                created_at = $runSuffix
            }
        }
        makeB = {
            param($id, $userId)
            return @{
                id = $id
                user_id = $userId
                title = "Rule-B-$runSuffix"
                type = "INCOME"
                cadence = "WEEKLY"
                next_run_at = $runSuffix + 200000
                amount = 456.0
                enabled = $false
                created_at = $runSuffix + 1
            }
        }
        marker = "title"
    }
)

foreach ($t in $tableTests) {
    $table = [string]$t.table
    $id = [int64]$t.id
    $payloadA = & $t.makeA $id $primary.userId
    $payloadB = & $t.makeB $id $secondary.userId
    $marker = [string]$t.marker
    $conflictTarget = if ($table -eq "merchant_categories") { "user_id,merchant" } else { "user_id,id" }

    $upsertA = Invoke-SupabaseRequest `
        -Method "POST" `
        -Path "/rest/v1/${table}?on_conflict=$conflictTarget" `
        -AccessToken $primary.token `
        -ExtraHeaders @{
            "Prefer" = "resolution=merge-duplicates,return=representation"
        } `
        -Body @($payloadA)
    Assert-True -Condition $upsertA.ok -Message "$table upsert for user A failed: HTTP $($upsertA.status) $($upsertA.content)"

    $upsertB = Invoke-SupabaseRequest `
        -Method "POST" `
        -Path "/rest/v1/${table}?on_conflict=$conflictTarget" `
        -AccessToken $secondary.token `
        -ExtraHeaders @{
            "Prefer" = "resolution=merge-duplicates,return=representation"
        } `
        -Body @($payloadB)
    Assert-True -Condition $upsertB.ok -Message "$table upsert for user B failed: HTTP $($upsertB.status) $($upsertB.content)"

    $readA = Invoke-SupabaseRequest `
        -Method "GET" `
        -Path "/rest/v1/${table}?select=*&id=eq.$id" `
        -AccessToken $primary.token
    Assert-True -Condition $readA.ok -Message "$table read for user A failed: HTTP $($readA.status) $($readA.content)"
    $rowsA = @(To-Array $readA.json)
    Assert-True -Condition ($rowsA.Count -eq 1) -Message "$table expected 1 row for user A, got $($rowsA.Count)."
    Assert-True -Condition ([string]$rowsA[0].user_id -eq $primary.userId) -Message "$table user A saw wrong owner."
    Assert-True -Condition ([string]$rowsA[0].$marker -eq [string]$payloadA.$marker) -Message "$table user A marker mismatch after user B upsert."

    $readB = Invoke-SupabaseRequest `
        -Method "GET" `
        -Path "/rest/v1/${table}?select=*&id=eq.$id" `
        -AccessToken $secondary.token
    Assert-True -Condition $readB.ok -Message "$table read for user B failed: HTTP $($readB.status) $($readB.content)"
    $rowsB = @(To-Array $readB.json)
    Assert-True -Condition ($rowsB.Count -eq 1) -Message "$table expected 1 row for user B, got $($rowsB.Count)."
    Assert-True -Condition ([string]$rowsB[0].user_id -eq $secondary.userId) -Message "$table user B saw wrong owner."
    Assert-True -Condition ([string]$rowsB[0].$marker -eq [string]$payloadB.$marker) -Message "$table user B marker mismatch."

    $crossRead = Invoke-SupabaseRequest `
        -Method "GET" `
        -Path "/rest/v1/${table}?select=id&user_id=eq.$($secondary.userId)&id=eq.$id" `
        -AccessToken $primary.token
    Assert-True -Condition $crossRead.ok -Message "$table cross-read query failed: HTTP $($crossRead.status) $($crossRead.content)"
    $crossRows = @(To-Array $crossRead.json)
    Assert-True -Condition ($crossRows.Count -eq 0) -Message "$table cross-read leak detected: user A can see user B row."

    Write-Host "${table}: PASS"
}

foreach ($t in $tableTests) {
    $table = [string]$t.table
    $id = [int64]$t.id

    $cleanupA = Invoke-SupabaseRequest `
        -Method "DELETE" `
        -Path "/rest/v1/${table}?id=eq.$id" `
        -AccessToken $primary.token `
        -ExtraHeaders @{
            "Prefer" = "return=minimal"
        }
    Assert-True -Condition $cleanupA.ok -Message "$table cleanup for user A failed: HTTP $($cleanupA.status) $($cleanupA.content)"

    $cleanupB = Invoke-SupabaseRequest `
        -Method "DELETE" `
        -Path "/rest/v1/${table}?id=eq.$id" `
        -AccessToken $secondary.token `
        -ExtraHeaders @{
            "Prefer" = "return=minimal"
        }
    Assert-True -Condition $cleanupB.ok -Message "$table cleanup for user B failed: HTTP $($cleanupB.status) $($cleanupB.content)"
}

Write-Host "Two-user Supabase smoke test passed for all synced tables."
Write-Host "Result: FULL_PASS"
