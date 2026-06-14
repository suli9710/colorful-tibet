param(
    [string]$HostName = "38.76.169.115",
    [string]$User = "root",
    [string]$RemoteProjectDir = "/opt/colorful-tibet",
    [string]$SiteUrl = "https://lengzhehao.xin/",
    [string]$ArchivePath = (Join-Path $env:TEMP "colorful-tibet-upload.tar.gz"),
    [switch]$SkipLocalChecks
)

$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $RepoRoot "scripts/New-DeploymentArchive.ps1")
$Remote = "${User}@${HostName}"
$RemoteArchive = "/tmp/colorful-tibet-upload.tar.gz"
$RemoteScript = "/tmp/colorful-tibet-upload.sh"
$LocalRemoteScript = Join-Path $env:TEMP "colorful-tibet-upload.sh"
$SshOptions = @("-o", "StrictHostKeyChecking=yes")

function Invoke-Native {
    param(
        [string]$Command,
        [string[]]$Arguments,
        [string]$WorkingDirectory = $RepoRoot
    )

    Push-Location $WorkingDirectory
    try {
        & $Command @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "$Command failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}

function Assert-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Missing required command: $Name"
    }
}

function ConvertTo-ShellSingleQuoted {
    param([string]$Value)
    return "'" + ($Value -replace "'", "'\''") + "'"
}

Assert-Command "tar"
Assert-Command "git"
Assert-Command "scp"
Assert-Command "ssh"

if (-not $SkipLocalChecks) {
    Write-Host "Running frontend typecheck..." -ForegroundColor Cyan
    Invoke-Native "npm" @("run", "typecheck") (Join-Path $RepoRoot "frontend")

    Write-Host "Running frontend build..." -ForegroundColor Cyan
    Invoke-Native "npm" @("run", "build") (Join-Path $RepoRoot "frontend")

    Write-Host "Running backend tests..." -ForegroundColor Cyan
    Invoke-Native "mvn" @("-q", "test") (Join-Path $RepoRoot "backend")
}
else {
    Write-Host "Skipping local checks." -ForegroundColor Yellow
}

if (Test-Path $ArchivePath) {
    Remove-Item -LiteralPath $ArchivePath -Force
}

Write-Host "Packing project from Git-tracked allowlist..." -ForegroundColor Cyan
New-DeploymentArchive -RepoRoot $RepoRoot -ArchivePath $ArchivePath

$projectDirLiteral = ConvertTo-ShellSingleQuoted $RemoteProjectDir
$archiveLiteral = ConvertTo-ShellSingleQuoted $RemoteArchive
$remoteScriptLiteral = ConvertTo-ShellSingleQuoted $RemoteScript
$siteUrlLiteral = ConvertTo-ShellSingleQuoted $SiteUrl

$remoteScriptContent = @'
#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR=__PROJECT_DIR__
ARCHIVE=__ARCHIVE__
REMOTE_SCRIPT=__REMOTE_SCRIPT__
SITE_URL=__SITE_URL__
RELEASE_DIR=$(mktemp -d /tmp/colorful-tibet-release.XXXXXX)
PREBUILD_PROJECT="colorful-tibet-prebuild"
ALLOWED_PROJECT_ROOT="/opt/colorful-tibet"

cleanup() {
  if [ -f "$RELEASE_DIR/docker-compose.prod.yml" ]; then
    (cd "$RELEASE_DIR" && docker compose -p "$PREBUILD_PROJECT" -f docker-compose.prod.yml down --rmi local --remove-orphans >/dev/null 2>&1) || true
  fi
  rm -rf "$RELEASE_DIR"
}
trap cleanup EXIT

validate_project_dir() {
  if [ -z "$PROJECT_DIR" ] || [ "${PROJECT_DIR#/}" = "$PROJECT_DIR" ]; then
    echo "Refusing non-absolute project path: $PROJECT_DIR" >&2
    exit 1
  fi

  case "$PROJECT_DIR" in
    *"/.."|*"/../"*|".."|"../"*|*"/."|*"/./"*|"."|"./"*|"//"*)
      echo "Refusing non-normalized project path: $PROJECT_DIR" >&2
      exit 1
      ;;
  esac

  if ! command -v realpath >/dev/null 2>&1; then
    echo "Refusing deployment: realpath is required for safe project path validation." >&2
    exit 1
  fi

  RESOLVED_PROJECT_DIR=$(realpath -m -- "$PROJECT_DIR")
  case "$RESOLVED_PROJECT_DIR" in
    "$ALLOWED_PROJECT_ROOT"|"$ALLOWED_PROJECT_ROOT"/*) PROJECT_DIR="$RESOLVED_PROJECT_DIR" ;;
    *) echo "Refusing project path outside $ALLOWED_PROJECT_ROOT: $PROJECT_DIR resolves to $RESOLVED_PROJECT_DIR" >&2; exit 1 ;;
  esac
}

ensure_project_dir() {
  mkdir -p "$PROJECT_DIR"
  RESOLVED_PROJECT_DIR=$(realpath -e -- "$PROJECT_DIR")
  case "$RESOLVED_PROJECT_DIR" in
    "$ALLOWED_PROJECT_ROOT"|"$ALLOWED_PROJECT_ROOT"/*) PROJECT_DIR="$RESOLVED_PROJECT_DIR" ;;
    *) echo "Refusing project path outside $ALLOWED_PROJECT_ROOT after creation: $RESOLVED_PROJECT_DIR" >&2; exit 1 ;;
  esac
}

reject_project_symlinks() {
  local name
  for name in "$@"; do
    if [ -L "$PROJECT_DIR/$name" ]; then
      echo "Refusing deployment: $PROJECT_DIR/$name must not be a symlink." >&2
      exit 1
    fi
  done
}

enter_project_dir() {
  cd -P -- "$PROJECT_DIR"
  CURRENT_PROJECT_DIR=$(pwd -P)
  if [ "$CURRENT_PROJECT_DIR" != "$PROJECT_DIR" ]; then
    echo "Refusing deployment: physical project directory changed from $PROJECT_DIR to $CURRENT_PROJECT_DIR." >&2
    exit 1
  fi
}

validate_project_dir
ensure_project_dir
COMPOSE_FILE="$PROJECT_DIR/docker-compose.prod.yml"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not installed on the server." >&2
  exit 1
fi

reject_project_symlinks ".env" data logs
mkdir -p "$PROJECT_DIR/data" "$PROJECT_DIR/logs"

if [ ! -f "$PROJECT_DIR/.env" ]; then
  echo "Missing $PROJECT_DIR/.env. Create production env first; upload will not invent secrets." >&2
  exit 1
fi

BACKEND_APP_UID=$(grep -E '^BACKEND_APP_UID=' "$PROJECT_DIR/.env" | tail -n 1 | cut -d= -f2- || true)
BACKEND_APP_GID=$(grep -E '^BACKEND_APP_GID=' "$PROJECT_DIR/.env" | tail -n 1 | cut -d= -f2- || true)
BACKEND_APP_UID=${BACKEND_APP_UID:-10001}
BACKEND_APP_GID=${BACKEND_APP_GID:-10001}
case "$BACKEND_APP_UID" in ''|*[!0-9]*) echo "Invalid BACKEND_APP_UID: $BACKEND_APP_UID" >&2; exit 1 ;; esac
case "$BACKEND_APP_GID" in ''|*[!0-9]*) echo "Invalid BACKEND_APP_GID: $BACKEND_APP_GID" >&2; exit 1 ;; esac

DNS_HOST_PATTERN='^[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?(\.[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*$'

is_placeholder_secret() {
  printf '%s' "$1" | grep -Eiq 'change-me|changeme|replace-with|placeholder|example\.com|local-dev|local-'
}

is_dns_host_value() {
  printf '%s' "$1" | grep -Eq "$DNS_HOST_PATTERN"
}

require_single_host_value() {
  local name="$1"
  local value="$2"
  if [ -z "$value" ]; then
    echo "Refusing deployment: $name must be configured." >&2
    exit 1
  fi
  if ! is_dns_host_value "$value"; then
    echo "Refusing deployment: $name must be a single DNS host without scheme, path, port, comma, whitespace, or control characters." >&2
    exit 1
  fi
  if is_placeholder_secret "$value"; then
    echo "Refusing deployment: $name must not contain placeholder values." >&2
    exit 1
  fi
}

require_server_name_list_value() {
  local name="$1"
  local value="$2"
  if [ -z "$value" ]; then
    echo "Refusing deployment: $name must be configured." >&2
    exit 1
  fi
  if printf '%s' "$value" | grep -Eq '[[:cntrl:],/]'; then
    echo "Refusing deployment: $name entries must be DNS hosts separated by single spaces only." >&2
    exit 1
  fi
  if is_placeholder_secret "$value"; then
    echo "Refusing deployment: $name must not contain placeholder values." >&2
    exit 1
  fi
  for host_name in $value; do
    if ! is_dns_host_value "$host_name"; then
      echo "Refusing deployment: $name entry '$host_name' must be a DNS host without scheme, path, port, comma, wildcard, or control characters." >&2
      exit 1
    fi
  done
}

NGINX_SERVER_NAME=$(grep -E '^NGINX_SERVER_NAME=' "$PROJECT_DIR/.env" | tail -n 1 | cut -d= -f2- || true)
NGINX_REDIRECT_HOST=$(grep -E '^NGINX_REDIRECT_HOST=' "$PROJECT_DIR/.env" | tail -n 1 | cut -d= -f2- || true)
NGINX_CERT_DOMAIN=$(grep -E '^NGINX_CERT_DOMAIN=' "$PROJECT_DIR/.env" | tail -n 1 | cut -d= -f2- || true)
require_server_name_list_value NGINX_SERVER_NAME "$NGINX_SERVER_NAME"
require_single_host_value NGINX_REDIRECT_HOST "$NGINX_REDIRECT_HOST"
require_single_host_value NGINX_CERT_DOMAIN "$NGINX_CERT_DOMAIN"

LE_CERT_DIR="/etc/letsencrypt/live/$NGINX_CERT_DOMAIN"
for cert_file in fullchain.pem privkey.pem chain.pem; do
  if [ ! -f "$LE_CERT_DIR/$cert_file" ]; then
    echo "Refusing deployment: missing Let's Encrypt certificate file $LE_CERT_DIR/$cert_file." >&2
    exit 1
  fi
done

get_env_value() {
  local name="$1"
  grep -E "^${name}=" "$PROJECT_DIR/.env" | tail -n 1 | cut -d= -f2- || true
}

require_real_secret() {
  local name="$1"
  local value
  value=$(get_env_value "$name")
  if [ -z "$value" ]; then
    echo "Refusing deployment: $name must be configured for production." >&2
    exit 1
  fi
  if is_placeholder_secret "$value"; then
    echo "Refusing deployment: $name must not contain placeholder values." >&2
    exit 1
  fi
}

reject_placeholder_secret_if_present() {
  local name="$1"
  local value
  value=$(get_env_value "$name")
  if [ -n "$value" ] && is_placeholder_secret "$value"; then
    echo "Refusing deployment: $name must not contain placeholder values." >&2
    exit 1
  fi
}

require_exact_env() {
  local name="$1"
  local expected="$2"
  local actual
  actual=$(get_env_value "$name")
  if [ -z "$actual" ]; then
    actual="$expected"
  fi
  if [ "$actual" != "$expected" ]; then
    echo "Refusing deployment: $name must be $expected for production, got '$actual'." >&2
    exit 1
  fi
}

for secret_name in \
  DB_PASSWORD \
  MYSQL_ROOT_PASSWORD \
  REDIS_PASSWORD \
  GRAFANA_ADMIN_PASSWORD \
  JWT_SECRET \
  ADMIN_ENCRYPTION_KEY \
  CSRF_SIGNING_SECRET \
  CACHE_KEY_HMAC_SECRET \
  PAYMENT_CALLBACK_SECRET \
  PII_KEYS \
  PII_ACTIVE_KID \
  SUPER_ADMIN_TOTP_SECRET \
  SCRAPLING_API_KEY \
  RECAPTCHA_SITE_KEY \
  RECAPTCHA_SECRET_KEY \
  VITE_AMAP_KEY \
  VITE_AMAP_SECURITY_CODE \
  ALERTMANAGER_WEBHOOK_URL; do
  require_real_secret "$secret_name"
done
reject_placeholder_secret_if_present MYSQL_PASSWORD
CACHE_KEY_HMAC_SECRET_VALUE=$(get_env_value CACHE_KEY_HMAC_SECRET)
if [ "${#CACHE_KEY_HMAC_SECRET_VALUE}" -lt 64 ]; then
  echo "Refusing deployment: CACHE_KEY_HMAC_SECRET must be at least 64 characters." >&2
  exit 1
fi

require_exact_env SPRING_PROFILES_ACTIVE prod
require_exact_env SPRING_FLYWAY_ENABLED true
require_exact_env SPRING_JPA_HIBERNATE_DDL_AUTO validate
require_exact_env REQUIRE_STRONG_SECRETS true
require_exact_env COOKIE_SECURE true
require_exact_env PAYMENT_MOCK_CALLBACK_ENABLED false
require_exact_env SEED_DEMO_USERS false
require_exact_env SEED_CONTENT_ENABLED false
require_exact_env DB_ALLOW_PUBLIC_KEY_RETRIEVAL false
require_exact_env PUBLIC_METRICS_ENABLED false
require_exact_env RATE_LIMIT_REDIS_ENABLED true
require_exact_env RATE_LIMIT_REDIS_FAIL_CLOSED true
require_exact_env BRUTE_FORCE_REDIS_ENABLED true
require_exact_env BRUTE_FORCE_REDIS_FAIL_CLOSED true
require_exact_env ANTIBOT_ENABLED true
require_exact_env RECAPTCHA_ENABLED true
require_exact_env REGISTRATION_RECAPTCHA_REQUIRED true
require_exact_env SCRAPLING_ALLOW_UNAUTHENTICATED false

TRUST_PROXY_HEADERS_VALUE=$(get_env_value TRUST_PROXY_HEADERS | tr '[:upper:]' '[:lower:]')
TRUSTED_PROXY_CIDRS_VALUE=$(get_env_value TRUSTED_PROXY_CIDRS | tr -d '[:space:]')
if [ "$TRUST_PROXY_HEADERS_VALUE" = "true" ]; then
  if [ -z "$TRUSTED_PROXY_CIDRS_VALUE" ]; then
    echo "Refusing deployment: TRUSTED_PROXY_CIDRS must be explicit when TRUST_PROXY_HEADERS=true." >&2
    exit 1
  fi
  if printf '%s' "$TRUSTED_PROXY_CIDRS_VALUE" | tr ',' '\n' | grep -Exq '0\.0\.0\.0/0|::/0|10\.0\.0\.0/8|172\.16\.0\.0/12|192\.168\.0\.0/16'; then
    echo "Refusing deployment: TRUSTED_PROXY_CIDRS must not trust broad private ranges." >&2
    exit 1
  fi
fi

DB_SSL_MODE_VALUE=$(get_env_value DB_SSL_MODE)
DB_SSL_MODE_VALUE=${DB_SSL_MODE_VALUE:-REQUIRED}
case "$DB_SSL_MODE_VALUE" in
  REQUIRED|VERIFY_IDENTITY) ;;
  *) echo "Refusing deployment: DB_SSL_MODE must be REQUIRED or VERIFY_IDENTITY for production, got '$DB_SSL_MODE_VALUE'." >&2; exit 1 ;;
esac

if ! grep -Eq '^(DOUBAO_API_KEY|ARK_API_KEY)=[^[:space:]]+' "$PROJECT_DIR/.env"; then
  echo "WARNING: DOUBAO_API_KEY/ARK_API_KEY is empty; AI route generation will use local fallback routes." >&2
fi

if grep -qx 'SUPER_ADMIN_SECONDARY_PASSWORD=lzh031224' "$PROJECT_DIR/.env"; then
  echo "Refusing deployment: remove the legacy SUPER_ADMIN_SECONDARY_PASSWORD published default before uploading." >&2
  exit 1
fi

if grep -qx 'SEED_DEMO_SUPER_ADMIN_PASSWORD=031224' "$PROJECT_DIR/.env"; then
  echo "Refusing deployment: SEED_DEMO_SUPER_ADMIN_PASSWORD still uses the published default. Rotate or remove it before uploading." >&2
  exit 1
fi

if ! grep -Eq '^SUPER_ADMIN_TOTP_SECRET=[A-Z2-7]{32,}$' "$PROJECT_DIR/.env"; then
  echo "Refusing deployment: SUPER_ADMIN_TOTP_SECRET must be a Base32 secret with at least 32 characters." >&2
  exit 1
fi

if grep -Eiq '^SPRING_JPA_HIBERNATE_DDL_AUTO=update$' "$PROJECT_DIR/.env"; then
  echo "Refusing deployment: production must not use SPRING_JPA_HIBERNATE_DDL_AUTO=update." >&2
  exit 1
fi

if ! grep -Eq '^PII_ACTIVE_KID=[A-Za-z0-9_.-]+$' "$PROJECT_DIR/.env"; then
  echo "Refusing deployment: PII_ACTIVE_KID must be configured with a simple key id." >&2
  exit 1
fi

ACTIVE_PII_KID=$(grep -E '^PII_ACTIVE_KID=' "$PROJECT_DIR/.env" | tail -n 1 | cut -d= -f2-)
PII_KEYS_VALUE=$(grep -E '^PII_KEYS=' "$PROJECT_DIR/.env" | tail -n 1 | cut -d= -f2- || true)
if [ -z "$PII_KEYS_VALUE" ]; then
  echo "Refusing deployment: PII_KEYS must be configured as kid:base64-32-byte-key entries." >&2
  exit 1
fi

if printf '%s' "$PII_KEYS_VALUE" | grep -Eiq 'change-me|changeme|replace-with|placeholder'; then
  echo "Refusing deployment: PII_KEYS must not contain placeholder values." >&2
  exit 1
fi

PII_ACTIVE_KEY=$(printf '%s' "$PII_KEYS_VALUE" | tr ',' '\n' | awk -F: -v kid="$ACTIVE_PII_KID" '$1 == kid {print $2; exit}')
if [ -z "$PII_ACTIVE_KEY" ]; then
  echo "Refusing deployment: PII_ACTIVE_KID must match an entry in PII_KEYS." >&2
  exit 1
fi

if [ "$(printf '%s' "$PII_ACTIVE_KEY" | base64 -d 2>/dev/null | wc -c | tr -d ' ')" != "32" ]; then
  echo "Refusing deployment: active PII key must decode to exactly 32 bytes." >&2
  exit 1
fi

PII_LEGACY_KEY_VALUE=$(grep -E '^PII_ENCRYPTION_KEY=' "$PROJECT_DIR/.env" | tail -n 1 | cut -d= -f2- || true)
if [ -n "$PII_LEGACY_KEY_VALUE" ] && printf '%s' "$PII_LEGACY_KEY_VALUE" | grep -Eiq 'change-me|changeme|replace-with|placeholder'; then
  echo "Refusing deployment: PII_ENCRYPTION_KEY must not contain placeholder values when legacy v1 PII rows need it." >&2
  exit 1
fi

echo "Prebuilding release before stopping current containers..."
tar -xzf "$ARCHIVE" -C "$RELEASE_DIR"
cp "$PROJECT_DIR/.env" "$RELEASE_DIR/.env"
mkdir -p "$RELEASE_DIR/certs/$NGINX_CERT_DOMAIN"
cp "$LE_CERT_DIR/fullchain.pem" "$LE_CERT_DIR/privkey.pem" "$LE_CERT_DIR/chain.pem" "$RELEASE_DIR/certs/$NGINX_CERT_DOMAIN/"
chown -R 101:101 "$RELEASE_DIR/certs"
chmod 750 "$RELEASE_DIR/certs" "$RELEASE_DIR/certs/$NGINX_CERT_DOMAIN"
chmod 640 "$RELEASE_DIR/certs/$NGINX_CERT_DOMAIN/"*.pem
cd "$RELEASE_DIR"
docker compose -f docker-compose.prod.yml config --quiet
COMPOSE_PROGRESS=plain BUILDKIT_PROGRESS=plain docker compose -p "$PREBUILD_PROJECT" -f docker-compose.prod.yml build

echo "Stopping current containers..."
if [ -f "$COMPOSE_FILE" ]; then
  docker compose -f "$COMPOSE_FILE" down
fi

echo "Replacing app files while preserving .env, data, and logs..."
reject_project_symlinks ".env" data logs
enter_project_dir
find . -mindepth 1 -maxdepth 1 \
  ! -name '.env' \
  ! -name 'data' \
  ! -name 'logs' \
  -exec rm -rf -- {} +

tar -xzf "$ARCHIVE" -C "$PROJECT_DIR"
reject_project_symlinks ".env" data logs certs
mkdir -p "$PROJECT_DIR/certs/$NGINX_CERT_DOMAIN"
cp "$LE_CERT_DIR/fullchain.pem" "$LE_CERT_DIR/privkey.pem" "$LE_CERT_DIR/chain.pem" "$PROJECT_DIR/certs/$NGINX_CERT_DOMAIN/"
chown -R 101:101 "$PROJECT_DIR/certs"
chmod 750 "$PROJECT_DIR/certs" "$PROJECT_DIR/certs/$NGINX_CERT_DOMAIN"
chmod 640 "$PROJECT_DIR/certs/$NGINX_CERT_DOMAIN/"*.pem
find "$PROJECT_DIR" -mindepth 1 \
  \( -path "$PROJECT_DIR/.env" -o -path "$PROJECT_DIR/certs" -o -path "$PROJECT_DIR/certs/*" -o -path "$PROJECT_DIR/data" -o -path "$PROJECT_DIR/data/*" -o -path "$PROJECT_DIR/logs" -o -path "$PROJECT_DIR/logs/*" \) -prune -o \
  -type d -exec chmod 755 {} +
find "$PROJECT_DIR" -mindepth 1 \
  \( -path "$PROJECT_DIR/.env" -o -path "$PROJECT_DIR/certs" -o -path "$PROJECT_DIR/certs/*" -o -path "$PROJECT_DIR/data" -o -path "$PROJECT_DIR/data/*" -o -path "$PROJECT_DIR/logs" -o -path "$PROJECT_DIR/logs/*" \) -prune -o \
  -type f -exec chmod 644 {} +
chown -R "$BACKEND_APP_UID:$BACKEND_APP_GID" "$PROJECT_DIR/data" "$PROJECT_DIR/logs"
find "$PROJECT_DIR/data" "$PROJECT_DIR/logs" -type d -exec chmod 750 {} +
find "$PROJECT_DIR/data" "$PROJECT_DIR/logs" -type f -exec chmod 640 {} +

cd "$PROJECT_DIR"
docker compose -f "$COMPOSE_FILE" config --quiet

echo "Building and starting containers..."
COMPOSE_PROGRESS=plain BUILDKIT_PROGRESS=plain docker compose -f "$COMPOSE_FILE" up -d --build

wait_for_health() {
  local container="$1"
  local timeout_seconds="${2:-300}"
  local elapsed=0
  local status=""

  while [ "$elapsed" -lt "$timeout_seconds" ]; do
    status=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || true)
    if [ "$status" = "healthy" ] || [ "$status" = "running" ]; then
      echo "$container is $status."
      return 0
    fi
    sleep 5
    elapsed=$((elapsed + 5))
  done

  echo "$container did not become healthy. Last status: $status" >&2
  docker compose -f "$COMPOSE_FILE" ps >&2 || true
  docker compose -f "$COMPOSE_FILE" logs --tail=160 backend frontend >&2 || true
  return 1
}

wait_for_health colorful-tibet-backend 360
wait_for_health colorful-tibet-frontend 180

docker exec colorful-tibet-frontend nginx -t
curl -fsS http://127.0.0.1:8080/actuator/health/readiness
echo ""
curl -fsS http://127.0.0.1:80/health
curl -fsS -I "$SITE_URL" | head -n 12

echo "Pruning unused Docker build cache..."
docker builder prune -af >/dev/null 2>&1 || true

rm -f "$ARCHIVE" "$REMOTE_SCRIPT"
echo "Upload deployment completed."
'@

$remoteScriptContent = $remoteScriptContent.Replace("__PROJECT_DIR__", $projectDirLiteral)
$remoteScriptContent = $remoteScriptContent.Replace("__ARCHIVE__", $archiveLiteral)
$remoteScriptContent = $remoteScriptContent.Replace("__REMOTE_SCRIPT__", $remoteScriptLiteral)
$remoteScriptContent = $remoteScriptContent.Replace("__SITE_URL__", $siteUrlLiteral)
$remoteScriptContent = $remoteScriptContent -replace "`r`n", "`n"
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($LocalRemoteScript, $remoteScriptContent, $utf8NoBom)

Write-Host "Uploading archive and remote runner..." -ForegroundColor Cyan
Invoke-Native "scp" ($SshOptions + @($ArchivePath, $LocalRemoteScript, "${Remote}:/tmp/")) $RepoRoot

Write-Host "Running server deployment..." -ForegroundColor Cyan
Invoke-Native "ssh" ($SshOptions + @($Remote, "bash $RemoteScript")) $RepoRoot

Write-Host "Checking public site..." -ForegroundColor Cyan
if (Get-Command "curl.exe" -ErrorAction SilentlyContinue) {
    Invoke-Native "curl.exe" @("-I", "--max-time", "20", $SiteUrl) $RepoRoot
}

Remove-Item -LiteralPath $ArchivePath -Force -ErrorAction SilentlyContinue
Remove-Item -LiteralPath $LocalRemoteScript -Force -ErrorAction SilentlyContinue

Write-Host "Done. Uploaded to $SiteUrl" -ForegroundColor Green
