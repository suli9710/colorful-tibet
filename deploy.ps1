#Requires -Version 5.1

[CmdletBinding()]
param(
    [string]$HostName = "lengzhehao.xin",
    [string[]]$Domains = @("lengzhehao.xin", "www.lengzhehao.xin"),
    [string]$User = "root",
    [string]$RemoteProjectDir = "/opt/colorful-tibet",
    [string]$ComposeFile = "docker-compose.prod.yml",
    [string]$ArchivePath = (Join-Path $env:TEMP "colorful-tibet-deploy.tar.gz"),
    [string]$CertName = "lengzhehao.xin",
    [string]$CertbotEmail = "",
    [switch]$SkipChecks,
    [switch]$SkipFrontendChecks,
    [switch]$SkipBackendTests,
    [switch]$SkipPublicVerify,
    [switch]$SkipHttpsSetup,
    [switch]$SkipRenewalDryRun
)

$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$Remote = "${User}@${HostName}"
$RemoteArchive = "/tmp/colorful-tibet-deploy.tar.gz"
$RemoteScript = "/tmp/colorful-tibet-remote-deploy.sh"
$LocalRemoteScript = Join-Path $env:TEMP "colorful-tibet-remote-deploy.sh"

if (-not $Domains -or $Domains.Count -eq 0) {
    $Domains = @($HostName)
}

if ([string]::IsNullOrWhiteSpace($CertName)) {
    $CertName = $Domains[0]
}

$PrimaryDomain = $Domains[0]

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Resolve-NativeCommand {
    param([string]$Name)

    $isWindowsHost = $env:OS -eq "Windows_NT"
    if ($isWindowsHost) {
        $cmdCommand = Get-Command "$Name.cmd" -ErrorAction SilentlyContinue
        if ($cmdCommand) {
            return $cmdCommand.Source
        }
    }

    $command = Get-Command $Name -ErrorAction Stop
    return $command.Source
}

function Invoke-Native {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$WorkingDirectory = $RepoRoot
    )

    Push-Location $WorkingDirectory
    try {
        & $FilePath @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "Command failed with exit code ${LASTEXITCODE}: $FilePath $($Arguments -join ' ')"
        }
    } finally {
        Pop-Location
    }
}

function ConvertTo-ShellSingleQuoted {
    param([string]$Value)
    return "'" + $Value.Replace("'", "'`"`"'") + "'"
}

function ConvertTo-BashBool {
    param([bool]$Value)
    if ($Value) {
        return "true"
    }
    return "false"
}

function ConvertTo-BashArrayLiteral {
    param([string[]]$Values)
    return "(" + (($Values | ForEach-Object { ConvertTo-ShellSingleQuoted $_ }) -join " ") + ")"
}

function Assert-RequiredCommand {
    param([string]$Name)
    $null = Get-Command $Name -ErrorAction Stop
}

Write-Step "Checking local prerequisites"
Assert-RequiredCommand "tar"
Assert-RequiredCommand "scp"
Assert-RequiredCommand "ssh"

if (-not (Test-Path (Join-Path $RepoRoot $ComposeFile))) {
    throw "Cannot find $ComposeFile in $RepoRoot"
}

if (-not $SkipChecks) {
    if (-not $SkipFrontendChecks) {
        $npm = Resolve-NativeCommand "npm"
        Write-Step "Running frontend typecheck"
        Invoke-Native $npm @("run", "typecheck") (Join-Path $RepoRoot "frontend")

        Write-Step "Running frontend build"
        Invoke-Native $npm @("run", "build") (Join-Path $RepoRoot "frontend")
    }

    if (-not $SkipBackendTests) {
        $mvn = Resolve-NativeCommand "mvn"
        Write-Step "Running backend tests"
        Invoke-Native $mvn @("-q", "test") (Join-Path $RepoRoot "backend")
    }
} else {
    Write-Host "Skipping local checks because -SkipChecks was supplied." -ForegroundColor Yellow
}

Write-Step "Creating deployment archive"
if (Test-Path $ArchivePath) {
    Remove-Item -LiteralPath $ArchivePath -Force
}

$tarExcludes = @(
    "--exclude=./.git",
    "--exclude=./.claude",
    "--exclude=./.cursor",
    "--exclude=./.vscode",
    "--exclude=./frontend/node_modules",
    "--exclude=./frontend/dist",
    "--exclude=./frontend/.vite",
    "--exclude=./backend/target",
    "--exclude=./backend/logs",
    "--exclude=./backend/uploads",
    "--exclude=./scrapler/.venv",
    "--exclude=./logs",
    "--exclude=./output",
    "--exclude=./.env",
    "--exclude=./scrapler/.env"
)

Invoke-Native "tar" ($tarExcludes + @("-czf", $ArchivePath, ".")) $RepoRoot
$archiveInfo = Get-Item -LiteralPath $ArchivePath
Write-Host ("Archive: {0} ({1:N1} MB)" -f $archiveInfo.FullName, ($archiveInfo.Length / 1MB))

Write-Step "Preparing remote deployment script"
$projectDirLiteral = ConvertTo-ShellSingleQuoted $RemoteProjectDir
$composeFileLiteral = ConvertTo-ShellSingleQuoted $ComposeFile
$remoteArchiveLiteral = ConvertTo-ShellSingleQuoted $RemoteArchive
$remoteScriptLiteral = ConvertTo-ShellSingleQuoted $RemoteScript
$domainsLiteral = ConvertTo-BashArrayLiteral $Domains
$certNameLiteral = ConvertTo-ShellSingleQuoted $CertName
$certbotEmailLiteral = ConvertTo-ShellSingleQuoted $CertbotEmail
$skipHttpsSetupLiteral = ConvertTo-BashBool ([bool]$SkipHttpsSetup)
$skipRenewalDryRunLiteral = ConvertTo-BashBool ([bool]$SkipRenewalDryRun)

$remoteScriptTemplate = @'
#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR=__PROJECT_DIR__
COMPOSE_FILE=__COMPOSE_FILE__
ARCHIVE=__ARCHIVE__
REMOTE_SCRIPT=__REMOTE_SCRIPT__
DOMAINS=__DOMAINS__
CERT_NAME=__CERT_NAME__
CERTBOT_EMAIL=__CERTBOT_EMAIL__
SKIP_HTTPS_SETUP=__SKIP_HTTPS_SETUP__
SKIP_RENEWAL_DRY_RUN=__SKIP_RENEWAL_DRY_RUN__

case "$PROJECT_DIR" in
  /opt/colorful-tibet|/opt/colorful-tibet/*) ;;
  *) echo "Unexpected project path: $PROJECT_DIR" >&2; exit 1 ;;
esac

if [ ! -f "$PROJECT_DIR/.env" ]; then
  echo "Missing $PROJECT_DIR/.env. Refusing to overwrite the deployment without production env." >&2
  exit 1
fi

if [ ! -f "$ARCHIVE" ]; then
  echo "Missing upload archive: $ARCHIVE" >&2
  exit 1
fi

set_env_value() {
  local key="$1"
  local value="$2"
  if grep -qE "^${key}=" .env; then
    sed -i "s#^${key}=.*#${key}=${value}#" .env
  else
    printf "\n%s=%s\n" "$key" "$value" >> .env
  fi
}

prepare_https() {
  if [ "$SKIP_HTTPS_SETUP" = "true" ]; then
    echo "Skipping HTTPS setup because -SkipHttpsSetup was supplied."
    return
  fi

  if [ "${#DOMAINS[@]}" -eq 0 ]; then
    echo "No domains configured for HTTPS." >&2
    exit 1
  fi

  mkdir -p /etc/letsencrypt /var/lib/letsencrypt /var/www/certbot
  set_env_value COOKIE_SECURE true
  set_env_value SERVER_FORWARD_HEADERS_STRATEGY framework

  if [ -f "/etc/letsencrypt/live/${CERT_NAME}/fullchain.pem" ] && [ -f "/etc/letsencrypt/live/${CERT_NAME}/privkey.pem" ]; then
    echo "Existing certificate found for ${CERT_NAME}; skipping initial issuance."
    return
  fi

  echo "No certificate found for ${CERT_NAME}; requesting Let's Encrypt certificate with standalone challenge..."
  local domain_args=()
  local domain
  for domain in "${DOMAINS[@]}"; do
    domain_args+=("-d" "$domain")
  done

  local account_args=(--register-unsafely-without-email)
  if [ -n "$CERTBOT_EMAIL" ]; then
    account_args=(--email "$CERTBOT_EMAIL" --no-eff-email)
  fi

  docker run --rm -p 80:80 \
    -v /etc/letsencrypt:/etc/letsencrypt \
    -v /var/lib/letsencrypt:/var/lib/letsencrypt \
    certbot/certbot certonly \
      --standalone \
      --non-interactive \
      --agree-tos \
      --cert-name "$CERT_NAME" \
      "${account_args[@]}" \
      "${domain_args[@]}"
}

wait_for_containers() {
  echo "Waiting for health checks..."
  for i in $(seq 1 90); do
    backend_status=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' colorful-tibet-backend 2>/dev/null || true)
    frontend_status=$(docker inspect -f '{{.State.Status}}' colorful-tibet-frontend 2>/dev/null || true)
    if [ "$backend_status" = "healthy" ] && [ "$frontend_status" = "running" ]; then
      echo "backend=$backend_status frontend=$frontend_status"
      return
    fi
    echo "waiting: backend=$backend_status frontend=$frontend_status"
    sleep 5
  done

  backend_status=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' colorful-tibet-backend 2>/dev/null || true)
  frontend_status=$(docker inspect -f '{{.State.Status}}' colorful-tibet-frontend 2>/dev/null || true)
  if [ "$backend_status" != "healthy" ] || [ "$frontend_status" != "running" ]; then
    docker compose -f "$COMPOSE_FILE" ps
    docker compose -f "$COMPOSE_FILE" logs --tail=160 backend
    exit 1
  fi
}

configure_certificate_renewal() {
  if [ "$SKIP_HTTPS_SETUP" = "true" ]; then
    return
  fi

  mkdir -p /var/www/certbot/.well-known/acme-challenge
  printf 'ok\n' > /var/www/certbot/.well-known/acme-challenge/deploy-healthcheck
  curl -fsS http://127.0.0.1/.well-known/acme-challenge/deploy-healthcheck >/dev/null

  local renewal_file="/etc/letsencrypt/renewal/${CERT_NAME}.conf"
  if [ -f "$renewal_file" ]; then
    local tmp_file="${renewal_file}.tmp"
    awk '
      /^\[\[webroot_map\]\]/ { in_map=1; next }
      in_map && /^\[/ { in_map=0 }
      in_map { next }
      /^authenticator =/ { print "authenticator = webroot"; auth_seen=1; next }
      /^webroot_path =/ { next }
      { print }
      END {
        if (!auth_seen) {
          print "authenticator = webroot"
        }
        print "webroot_path = /var/www/certbot,"
      }
    ' "$renewal_file" > "$tmp_file"
    {
      cat "$tmp_file"
      printf "\n[[webroot_map]]\n"
      local domain
      for domain in "${DOMAINS[@]}"; do
        printf "%s = /var/www/certbot\n" "$domain"
      done
    } > "$renewal_file"
    rm -f "$tmp_file"
  else
    echo "Warning: renewal file not found: $renewal_file" >&2
  fi

  cat >/etc/cron.d/colorful-tibet-certbot <<'CRON'
SHELL=/bin/sh
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
17 3 * * * root docker run --rm -v /etc/letsencrypt:/etc/letsencrypt -v /var/lib/letsencrypt:/var/lib/letsencrypt -v /var/www/certbot:/var/www/certbot certbot/certbot renew --quiet && docker exec colorful-tibet-frontend nginx -s reload
CRON
  chmod 644 /etc/cron.d/colorful-tibet-certbot

  if [ "$SKIP_RENEWAL_DRY_RUN" = "true" ]; then
    echo "Skipping certbot renewal dry-run because -SkipRenewalDryRun was supplied."
    return
  fi

  echo "Running certbot renewal dry-run..."
  if ! docker run --rm \
    -v /etc/letsencrypt:/etc/letsencrypt \
    -v /var/lib/letsencrypt:/var/lib/letsencrypt \
    -v /var/www/certbot:/var/www/certbot \
    certbot/certbot renew \
      --dry-run \
      --cert-name "$CERT_NAME" \
      --non-interactive \
      --no-random-sleep-on-renew \
      -v; then
    echo "Warning: certbot renewal dry-run failed. Deployment is running, but renewal should be checked manually." >&2
  fi
}

verify_local_site() {
  docker compose -f "$COMPOSE_FILE" ps
  docker exec colorful-tibet-frontend nginx -t
  curl -fsS http://127.0.0.1:8080/actuator/health/readiness
  echo ""

  if [ "$SKIP_HTTPS_SETUP" = "true" ]; then
    curl -fsS -I http://127.0.0.1/ | head -n 8
    return
  fi

  curl -fsS -I --resolve "${CERT_NAME}:443:127.0.0.1" "https://${CERT_NAME}/" | head -n 8
  curl -fsS -I --resolve "${CERT_NAME}:80:127.0.0.1" "http://${CERT_NAME}/" | head -n 8
}

cd "$PROJECT_DIR"
echo "Stopping current containers..."
docker compose -f "$COMPOSE_FILE" down

echo "Replacing application files while preserving .env, data, and logs..."
find "$PROJECT_DIR" -mindepth 1 -maxdepth 1 \
  ! -name '.env' \
  ! -name 'data' \
  ! -name 'logs' \
  -exec rm -rf -- {} +

tar -xzf "$ARCHIVE" -C "$PROJECT_DIR"
mkdir -p "$PROJECT_DIR/data" "$PROJECT_DIR/logs"
chmod -R a+rwX "$PROJECT_DIR/data" "$PROJECT_DIR/logs"

prepare_https

echo "Building and starting containers..."
COMPOSE_PROGRESS=plain BUILDKIT_PROGRESS=plain docker compose -f "$COMPOSE_FILE" up -d --build

wait_for_containers
configure_certificate_renewal
verify_local_site

rm -f "$ARCHIVE" "$REMOTE_SCRIPT"
echo "Remote deployment completed."
'@

$remoteScriptContent = $remoteScriptTemplate
$remoteScriptContent = $remoteScriptContent.Replace("__PROJECT_DIR__", $projectDirLiteral)
$remoteScriptContent = $remoteScriptContent.Replace("__COMPOSE_FILE__", $composeFileLiteral)
$remoteScriptContent = $remoteScriptContent.Replace("__ARCHIVE__", $remoteArchiveLiteral)
$remoteScriptContent = $remoteScriptContent.Replace("__REMOTE_SCRIPT__", $remoteScriptLiteral)
$remoteScriptContent = $remoteScriptContent.Replace("__DOMAINS__", $domainsLiteral)
$remoteScriptContent = $remoteScriptContent.Replace("__CERT_NAME__", $certNameLiteral)
$remoteScriptContent = $remoteScriptContent.Replace("__CERTBOT_EMAIL__", $certbotEmailLiteral)
$remoteScriptContent = $remoteScriptContent.Replace("__SKIP_HTTPS_SETUP__", $skipHttpsSetupLiteral)
$remoteScriptContent = $remoteScriptContent.Replace("__SKIP_RENEWAL_DRY_RUN__", $skipRenewalDryRunLiteral)

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($LocalRemoteScript, $remoteScriptContent, $utf8NoBom)

Write-Step "Uploading archive to $Remote"
Invoke-Native "scp" @($ArchivePath, $LocalRemoteScript, "${Remote}:/tmp/") $RepoRoot

Write-Step "Deploying on remote server"
Invoke-Native "ssh" @($Remote, "bash $RemoteScript") $RepoRoot

if (-not $SkipPublicVerify) {
    Write-Step "Verifying public site"
    if ($SkipHttpsSetup) {
        $url = "http://$PrimaryDomain/"
    } else {
        $url = "https://$PrimaryDomain/"
    }
    $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 20
    if ($response.StatusCode -lt 200 -or $response.StatusCode -ge 400) {
        throw "Public verification failed: $url returned $($response.StatusCode)"
    }
    Write-Host "Public site is reachable: $url" -ForegroundColor Green
}

Write-Step "Done"
if ($SkipHttpsSetup) {
    Write-Host "Deployed to http://$PrimaryDomain/" -ForegroundColor Green
} else {
    Write-Host "Deployed to https://$PrimaryDomain/" -ForegroundColor Green
}
