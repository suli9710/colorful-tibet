param(
    [ValidateSet("staging", "release")]
    [string] $Stage = "staging",

    [string] $EnvFile = ".env",
    [string] $ComposeFile = "docker-compose.prod.yml",
    [string] $DockerDigestEvidence = "docker-digest-evidence.json",

    [switch] $AllowPiiBackfill,
    [string] $PiiBackfillTicket = "",
    [switch] $SkipDigestEvidence,
    [switch] $SkipComposeConfig
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$Issues = New-Object System.Collections.Generic.List[string]

function Add-Issue {
    param([string] $Message)
    $script:Issues.Add($Message) | Out-Null
}

function Resolve-RepoPath {
    param([string] $PathValue)
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return $PathValue
    }
    return (Join-Path $RepoRoot $PathValue)
}

function Read-DotEnv {
    param([string] $PathValue)

    $result = @{}
    if (-not (Test-Path -LiteralPath $PathValue)) {
        Add-Issue "Environment file not found: $PathValue"
        return $result
    }

    $lineNumber = 0
    foreach ($line in Get-Content -LiteralPath $PathValue) {
        $lineNumber += 1
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#")) {
            continue
        }
        $match = [regex]::Match($line, '^\s*(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$')
        if (-not $match.Success) {
            Add-Issue "Cannot parse $PathValue line ${lineNumber}; line contents were suppressed."
            continue
        }

        $name = $match.Groups[1].Value
        $value = $match.Groups[2].Value.Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        $result[$name] = $value
    }

    return $result
}

function Get-EnvValue {
    param(
        [hashtable] $EnvValues,
        [string] $Name,
        [string] $Default = ""
    )

    if ($EnvValues.ContainsKey($Name)) {
        return [string] $EnvValues[$Name]
    }
    return $Default
}

function Test-Placeholder {
    param([string] $Value)
    return $Value -match '(?i)(replace-with|change-me|changeme|placeholder|example\.com|local-dev|local-)'
}

function Test-DnsHostName {
    param([string] $Value)
    return $Value -match '^[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?(\.[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*$'
}

function Require-Exact {
    param(
        [hashtable] $EnvValues,
        [string] $Name,
        [string] $Expected
    )

    $actual = Get-EnvValue $EnvValues $Name
    if ($actual -ne $Expected) {
        Add-Issue "$Name must be $Expected for $Stage."
    }
}

function Require-IntRange {
    param(
        [hashtable] $EnvValues,
        [string] $Name,
        [int] $Minimum,
        [int] $Maximum
    )

    $value = Get-EnvValue $EnvValues $Name
    $parsed = 0
    if (-not [int]::TryParse($value, [ref] $parsed) -or $parsed -lt $Minimum -or $parsed -gt $Maximum) {
        Add-Issue "$Name must be an integer between $Minimum and $Maximum for $Stage."
    }
}

function Require-RealValue {
    param(
        [hashtable] $EnvValues,
        [string] $Name
    )

    $value = Get-EnvValue $EnvValues $Name
    if ([string]::IsNullOrWhiteSpace($value)) {
        Add-Issue "$Name must be set for $Stage."
        return
    }
    if (Test-Placeholder $value) {
        Add-Issue "$Name still looks like a placeholder."
    }
}

function Require-SingleHost {
    param(
        [hashtable] $EnvValues,
        [string] $Name
    )

    $value = Get-EnvValue $EnvValues $Name
    if ([string]::IsNullOrWhiteSpace($value)) {
        Add-Issue "$Name must be set for $Stage."
        return
    }
    if (Test-Placeholder $value) {
        Add-Issue "$Name still looks like a placeholder."
    }
    if ($value -match '\s' -or -not (Test-DnsHostName $value)) {
        Add-Issue "$Name must be a single DNS host without scheme, path, port, comma, whitespace, or control characters."
    }
}

function Require-ServerNameList {
    param(
        [hashtable] $EnvValues,
        [string] $Name
    )

    $value = Get-EnvValue $EnvValues $Name
    if ([string]::IsNullOrWhiteSpace($value)) {
        Add-Issue "$Name must be set for $Stage."
        return
    }
    if (Test-Placeholder $value) {
        Add-Issue "$Name still looks like a placeholder."
    }
    if ($value -match '[\r\n\t]') {
        Add-Issue "$Name entries must be DNS hosts separated by single spaces only."
        return
    }

    foreach ($hostName in ($value.Trim() -split ' +')) {
        if (-not (Test-DnsHostName $hostName)) {
            Add-Issue "$Name contains an entry that is not a DNS host without scheme, path, port, comma, wildcard, or control characters."
        }
    }
}

function Require-Base64KeySet {
    param(
        [string] $PiiKeys,
        [string] $ActiveKid
    )

    if ([string]::IsNullOrWhiteSpace($PiiKeys)) {
        Add-Issue "PII_KEYS must be set."
        return
    }
    if ([string]::IsNullOrWhiteSpace($ActiveKid)) {
        Add-Issue "PII_ACTIVE_KID must be set."
        return
    }

    $foundActiveKid = $false
    foreach ($entry in $PiiKeys.Split(",")) {
        $parts = $entry.Split(":", 2)
        if ($parts.Count -ne 2 -or [string]::IsNullOrWhiteSpace($parts[0]) -or [string]::IsNullOrWhiteSpace($parts[1])) {
            Add-Issue "PII_KEYS contains an entry that must use kid:base64-32-byte-key format; entry contents were suppressed."
            continue
        }

        try {
            $bytes = [Convert]::FromBase64String($parts[1])
            if ($bytes.Length -ne 32) {
                Add-Issue "PII_KEYS contains a key that must decode to exactly 32 bytes; entry contents were suppressed."
            }
        } catch {
            Add-Issue "PII_KEYS contains a key that is not valid Base64; entry contents were suppressed."
        }

        if ($parts[0] -eq $ActiveKid) {
            $foundActiveKid = $true
        }
    }

    if (-not $foundActiveKid) {
        Add-Issue "PII_ACTIVE_KID '$ActiveKid' is not present in PII_KEYS."
    }
}

function Require-Base32TotpSecret {
    param(
        [hashtable] $EnvValues,
        [string] $Name
    )

    $value = Get-EnvValue $EnvValues $Name
    if ([string]::IsNullOrWhiteSpace($value)) {
        Add-Issue "$Name must be set for $Stage."
        return
    }
    if (Test-Placeholder $value) {
        Add-Issue "$Name still looks like a placeholder."
        return
    }
    if (-not (Test-StrongTotpSecret $value)) {
        Add-Issue "$Name must be a high-entropy Base32 secret in complete 8-character blocks with at least 32 characters."
    }
}

function Test-StrongTotpSecret {
    param([string] $Secret)

    if ($Secret -cnotmatch '^[A-Z2-7]{32,}$' -or ($Secret.Length % 8) -ne 0) {
        return $false
    }
    if (@($Secret.ToCharArray() | Sort-Object -Unique).Count -lt 8) {
        return $false
    }
    if ($Secret -in @(
        'GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ',
        'JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP',
        'MZXW6YTBOJQXGZJAMZXXE3DEMF2GK3LQ',
        'NBSWY3DPEB3W64TMMQXG6ZRAMZXXE3DE',
        'MFRGGZDFMZTWQ2LKMFRGGZDFMZTWQ2LK'
    )) {
        return $false
    }
    if ((Test-SequentialBase32Pattern $Secret) -or (Test-PeriodicTotpPattern $Secret)) {
        return $false
    }
    return $true
}

function Test-SequentialBase32Pattern {
    param([string] $CanonicalSecret)

    $alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'
    $ascending = $true
    $descending = $true
    for ($index = 1; $index -lt $CanonicalSecret.Length -and ($ascending -or $descending); $index++) {
        $previous = $alphabet.IndexOf($CanonicalSecret[$index - 1])
        $current = $alphabet.IndexOf($CanonicalSecret[$index])
        if ($current -ne (($previous + 1) % $alphabet.Length)) {
            $ascending = $false
        }
        if ($current -ne (($previous - 1 + $alphabet.Length) % $alphabet.Length)) {
            $descending = $false
        }
    }
    return $ascending -or $descending
}

function Test-PeriodicTotpPattern {
    param([string] $CanonicalSecret)

    for ($period = 1; $period -le [Math]::Floor($CanonicalSecret.Length / 2); $period++) {
        if (($CanonicalSecret.Length % $period) -ne 0) {
            continue
        }
        $repeated = $true
        for ($index = $period; $index -lt $CanonicalSecret.Length; $index++) {
            if ($CanonicalSecret[$index] -cne $CanonicalSecret[$index % $period]) {
                $repeated = $false
                break
            }
        }
        if ($repeated) {
            return $true
        }
    }
    return $false
}

function Validate-AdminTotpSecrets {
    param([hashtable] $EnvValues)

    # A blank map is valid for a deliberate super-admin-only installation. If operators add
    # entries, validate the complete map here so a typo cannot leave an administrator permanently
    # unable to sign in after the deployment has already stopped traffic.
    $raw = Get-EnvValue $EnvValues "ADMIN_TOTP_SECRETS"
    if ([string]::IsNullOrWhiteSpace($raw)) {
        return
    }
    if ($raw -match '[\r\n]' -or $raw.StartsWith(';') -or $raw.EndsWith(';') -or $raw.Contains(';;')) {
        Add-Issue "ADMIN_TOTP_SECRETS must use semicolon-separated username=Base32Secret entries without empty or control-character entries."
        return
    }

    $superUsername = (Get-EnvValue $EnvValues "SUPER_ADMIN_USERNAME" "lzh").Trim().ToLowerInvariant()
    $superSecret = Get-EnvValue $EnvValues "SUPER_ADMIN_TOTP_SECRET"
    $seenUsers = @{}
    $seenSecrets = @{}
    if ($superUsername.Length -gt 0) {
        $seenUsers[$superUsername] = $true
    }
    if ($superSecret.Length -gt 0) {
        $seenSecrets[$superSecret] = $true
    }

    foreach ($entry in $raw.Split(';')) {
        $parts = $entry.Split("=", 2)
        if ($parts.Count -ne 2) {
            Add-Issue "ADMIN_TOTP_SECRETS contains an invalid username=Base32Secret entry."
            continue
        }

        $username = $parts[0].Trim()
        $secret = $parts[1].Trim()
        if ($username -notmatch '^[A-Za-z0-9_-]{3,64}$') {
            Add-Issue "ADMIN_TOTP_SECRETS contains an invalid administrator username."
            continue
        }
        $usernameKey = $username.ToLowerInvariant()
        if ($seenUsers.ContainsKey($usernameKey)) {
            Add-Issue "ADMIN_TOTP_SECRETS contains a duplicate or super-admin username."
        } else {
            $seenUsers[$usernameKey] = $true
        }

        if ([string]::IsNullOrWhiteSpace($secret) -or (Test-Placeholder $secret)) {
            Add-Issue "ADMIN_TOTP_SECRETS contains an empty or placeholder secret."
            continue
        }
        if (-not (Test-StrongTotpSecret $secret)) {
            Add-Issue "Each ADMIN_TOTP_SECRETS secret must be a high-entropy Base32 secret in complete 8-character blocks with at least 32 characters."
            continue
        }
        if ($seenSecrets.ContainsKey($secret)) {
            Add-Issue "ADMIN_TOTP_SECRETS secrets must be independent."
        } else {
            $seenSecrets[$secret] = $true
        }
    }
}

function Invoke-CheckedCommand {
    param(
        [string] $FilePath,
        [string[]] $Arguments,
        [string] $FailureMessage
    )

    $output = & $FilePath @Arguments 2>&1
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) {
        Add-Issue "$FailureMessage`n$($output -join "`n")"
    }
}

$EnvPath = Resolve-RepoPath $EnvFile
$ComposePath = Resolve-RepoPath $ComposeFile
$EvidencePath = Resolve-RepoPath $DockerDigestEvidence
$EnvValues = Read-DotEnv $EnvPath

if (-not (Test-Path -LiteralPath $ComposePath)) {
    Add-Issue "Compose file not found: $ComposePath"
}

Require-Exact $EnvValues "SPRING_PROFILES_ACTIVE" "prod"
Require-Exact $EnvValues "REQUIRE_STRONG_SECRETS" "true"
Require-Exact $EnvValues "COOKIE_SECURE" "true"
Require-Exact $EnvValues "PAYMENT_MOCK_CALLBACK_ENABLED" "false"
Require-Exact $EnvValues "SEED_DEMO_USERS" "false"
Require-Exact $EnvValues "SEED_CONTENT_ENABLED" "false"
Require-Exact $EnvValues "SPRING_FLYWAY_ENABLED" "true"
Require-Exact $EnvValues "SPRING_JPA_HIBERNATE_DDL_AUTO" "validate"
Require-Exact $EnvValues "DB_ALLOW_PUBLIC_KEY_RETRIEVAL" "false"
Require-Exact $EnvValues "PUBLIC_METRICS_ENABLED" "false"
Require-Exact $EnvValues "RATE_LIMIT_REDIS_ENABLED" "true"
Require-Exact $EnvValues "RATE_LIMIT_REDIS_FAIL_CLOSED" "true"
Require-Exact $EnvValues "BRUTE_FORCE_REDIS_ENABLED" "true"
Require-Exact $EnvValues "BRUTE_FORCE_REDIS_FAIL_CLOSED" "true"
Require-Exact $EnvValues "TOTP_REPLAY_FAIL_CLOSED" "true"
Require-Exact $EnvValues "ANTIBOT_ENABLED" "true"
Require-Exact $EnvValues "ANTIBOT_LOG_RETENTION_ENABLED" "true"
Require-IntRange $EnvValues "ANTIBOT_LOG_RETENTION_DAYS" 1 365
Require-Exact $EnvValues "RECAPTCHA_ENABLED" "true"
Require-Exact $EnvValues "REGISTRATION_RECAPTCHA_REQUIRED" "true"
Require-Exact $EnvValues "SCRAPLING_ALLOW_UNAUTHENTICATED" "false"
Require-Exact $EnvValues "SERVER_FORWARD_HEADERS_STRATEGY" "none"
Require-Exact $EnvValues "TRUST_PROXY_HEADERS" "true"

$frontendContainerIp = Get-EnvValue $EnvValues "FRONTEND_CONTAINER_IP"
$trustedProxyCidrs = Get-EnvValue $EnvValues "TRUSTED_PROXY_CIDRS"
if ([string]::IsNullOrWhiteSpace($frontendContainerIp)) {
    Add-Issue "FRONTEND_CONTAINER_IP must be set for $Stage."
} elseif ($trustedProxyCidrs -cne "$frontendContainerIp/32") {
    Add-Issue "TRUSTED_PROXY_CIDRS must exactly match FRONTEND_CONTAINER_IP/32 for $Stage; expected '$frontendContainerIp/32', got '$trustedProxyCidrs'."
}

foreach ($name in @(
    "JWT_SECRET",
    "CSRF_SIGNING_SECRET",
    "CACHE_KEY_HMAC_SECRET",
    "METRICS_SCRAPE_TOKEN",
    "ADMIN_ENCRYPTION_KEY",
    "PAYMENT_CALLBACK_SECRET",
    "DB_PASSWORD",
    "MYSQL_ROOT_PASSWORD",
    "REDIS_PASSWORD",
    "SUPER_ADMIN_TOTP_SECRET",
    "SCRAPLING_API_KEY",
    "RECAPTCHA_SITE_KEY",
    "RECAPTCHA_SECRET_KEY",
    "VITE_AMAP_KEY",
    "VITE_AMAP_SECURITY_CODE",
    "GRAFANA_ADMIN_PASSWORD",
    "ALERTMANAGER_WEBHOOK_URL",
    "APP_VERSION"
)) {
    Require-RealValue $EnvValues $name
}

Require-ServerNameList $EnvValues "NGINX_SERVER_NAME"
Require-SingleHost $EnvValues "NGINX_REDIRECT_HOST"
Require-SingleHost $EnvValues "NGINX_CERT_DOMAIN"

$cacheKeyHmacSecret = Get-EnvValue $EnvValues "CACHE_KEY_HMAC_SECRET"
if ($cacheKeyHmacSecret.Length -lt 64) {
    Add-Issue "CACHE_KEY_HMAC_SECRET must be at least 64 characters for $Stage."
}

$metricsScrapeToken = Get-EnvValue $EnvValues "METRICS_SCRAPE_TOKEN"
if ($metricsScrapeToken.Length -lt 64) {
    Add-Issue "METRICS_SCRAPE_TOKEN must be at least 64 characters for $Stage."
}

$dbSslMode = Get-EnvValue $EnvValues "DB_SSL_MODE" "REQUIRED"
if ($dbSslMode -notin @("REQUIRED", "VERIFY_IDENTITY")) {
    Add-Issue "DB_SSL_MODE must be REQUIRED or VERIFY_IDENTITY for $Stage, got '$dbSslMode'."
}

Require-Base64KeySet (Get-EnvValue $EnvValues "PII_KEYS") (Get-EnvValue $EnvValues "PII_ACTIVE_KID")
Require-Base32TotpSecret $EnvValues "SUPER_ADMIN_TOTP_SECRET"
Validate-AdminTotpSecrets $EnvValues

$piiMigration = (Get-EnvValue $EnvValues "PII_MIGRATION_ENABLED" "false").ToLowerInvariant()
if ($piiMigration -eq "true") {
    if (-not $AllowPiiBackfill) {
        Add-Issue "PII_MIGRATION_ENABLED=true is blocked by default. Re-run with -AllowPiiBackfill only for the approved one-window PII backfill."
    }
    if ([string]::IsNullOrWhiteSpace($PiiBackfillTicket)) {
        Add-Issue "PII backfill requires -PiiBackfillTicket with the change/ticket ID and rollback owner."
    }
} elseif ($piiMigration -ne "false") {
    Add-Issue "PII_MIGRATION_ENABLED must be true or false, got '$piiMigration'."
}

$adminAuditMigration = Join-Path $RepoRoot "backend/src/main/resources/db/migration/V24__create_admin_audit_logs.sql"
if (-not (Test-Path -LiteralPath $adminAuditMigration)) {
    Add-Issue "Admin audit migration is missing: backend/src/main/resources/db/migration/V24__create_admin_audit_logs.sql"
}

if (-not $SkipDigestEvidence) {
    if (-not (Test-Path -LiteralPath $EvidencePath)) {
        Add-Issue "Docker digest evidence is missing: $EvidencePath. Generate it on a networked release workstation; do not invent digests offline."
    } else {
        Invoke-CheckedCommand "node" @(
            (Join-Path $RepoRoot "scripts/check-supply-chain-pins.mjs"),
            "--evidence-only",
            "--evidence",
            $EvidencePath,
            $RepoRoot
        ) "Docker digest evidence failed offline validation."
    }
}

if (-not $SkipComposeConfig -and (Test-Path -LiteralPath $ComposePath) -and (Test-Path -LiteralPath $EnvPath)) {
    Invoke-CheckedCommand "docker" @(
        "compose",
        "--env-file",
        $EnvPath,
        "-f",
        $ComposePath,
        "config",
        "--quiet"
    ) "docker compose config validation failed."
}

if ($Issues.Count -gt 0) {
    Write-Error ("Deployment preflight failed for {0}:`n- {1}" -f $Stage, ($Issues -join "`n- "))
    exit 1
}

Write-Host "Deployment preflight passed for $Stage."
Write-Host "Checked env: $EnvPath"
Write-Host "Checked compose: $ComposePath"
if (-not $SkipDigestEvidence) {
    Write-Host "Checked Docker digest evidence: $EvidencePath"
}
if ($piiMigration -eq "true") {
    Write-Host "PII backfill is explicitly enabled under ticket: $PiiBackfillTicket"
}
