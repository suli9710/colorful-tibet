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
$Remote = "${User}@${HostName}"
$RemoteArchive = "/tmp/colorful-tibet-upload.tar.gz"
$RemoteScript = "/tmp/colorful-tibet-upload.sh"
$LocalRemoteScript = Join-Path $env:TEMP "colorful-tibet-upload.sh"

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

$tarExcludes = @(
    "--exclude=./.idea",
    "--exclude=./.vscode",
    "--exclude=./node_modules",
    "--exclude=./frontend/node_modules",
    "--exclude=./frontend/dist",
    "--exclude=./backend/target",
    "--exclude=./backend/logs",
    "--exclude=./logs",
    "--exclude=./.startup",
    "--exclude=./certs",
    "--exclude=./scrapler/.venv",
    "--exclude=./.env",
    "--exclude=./.env.*",
    "--exclude=*/.env",
    "--exclude=*/.env.*",
    "--exclude=./*.log",
    "--exclude=./*.tar.gz"
)

$topLevelExcludes = @(".git", ".idea", ".vscode", "node_modules", "data", "logs", ".env", ".startup", "certs")
$tarIncludes = Get-ChildItem -LiteralPath $RepoRoot -Force |
    Where-Object {
        $topLevelExcludes -notcontains $_.Name -and
        $_.Name -notlike "*.tar.gz"
    } |
    ForEach-Object { "./$($_.Name)" }

if (-not $tarIncludes) {
    throw "No files found to upload."
}

Write-Host "Packing project..." -ForegroundColor Cyan
Invoke-Native "tar" ($tarExcludes + @("-czf", $ArchivePath) + $tarIncludes) $RepoRoot

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
COMPOSE_FILE="$PROJECT_DIR/docker-compose.prod.yml"
RELEASE_DIR=$(mktemp -d /tmp/colorful-tibet-release.XXXXXX)
PREBUILD_PROJECT="colorful-tibet-prebuild"

cleanup() {
  if [ -f "$RELEASE_DIR/docker-compose.prod.yml" ]; then
    (cd "$RELEASE_DIR" && docker compose -p "$PREBUILD_PROJECT" -f docker-compose.prod.yml down --rmi local --remove-orphans >/dev/null 2>&1) || true
  fi
  rm -rf "$RELEASE_DIR"
}
trap cleanup EXIT

case "$PROJECT_DIR" in
  /opt/colorful-tibet|/opt/colorful-tibet/*) ;;
  *) echo "Refusing unexpected project path: $PROJECT_DIR" >&2; exit 1 ;;
esac

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not installed on the server." >&2
  exit 1
fi

mkdir -p "$PROJECT_DIR/data" "$PROJECT_DIR/logs"

if [ ! -f "$PROJECT_DIR/.env" ]; then
  echo "Missing $PROJECT_DIR/.env. Create production env first; upload will not invent secrets." >&2
  exit 1
fi

NGINX_CERT_DOMAIN=$(grep -E '^NGINX_CERT_DOMAIN=' "$PROJECT_DIR/.env" | tail -n 1 | cut -d= -f2- || true)
if [ -z "$NGINX_CERT_DOMAIN" ]; then
  echo "Refusing deployment: NGINX_CERT_DOMAIN must be configured." >&2
  exit 1
fi
LE_CERT_DIR="/etc/letsencrypt/live/$NGINX_CERT_DOMAIN"
for cert_file in fullchain.pem privkey.pem chain.pem; do
  if [ ! -f "$LE_CERT_DIR/$cert_file" ]; then
    echo "Refusing deployment: missing Let's Encrypt certificate file $LE_CERT_DIR/$cert_file." >&2
    exit 1
  fi
done

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
find "$PROJECT_DIR" -mindepth 1 -maxdepth 1 \
  ! -name '.env' \
  ! -name 'data' \
  ! -name 'logs' \
  -exec rm -rf -- {} +

tar -xzf "$ARCHIVE" -C "$PROJECT_DIR"
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
chmod -R a+rwX "$PROJECT_DIR/data" "$PROJECT_DIR/logs"

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
Invoke-Native "scp" @($ArchivePath, $LocalRemoteScript, "${Remote}:/tmp/") $RepoRoot

Write-Host "Running server deployment..." -ForegroundColor Cyan
Invoke-Native "ssh" @($Remote, "bash $RemoteScript") $RepoRoot

Write-Host "Checking public site..." -ForegroundColor Cyan
if (Get-Command "curl.exe" -ErrorAction SilentlyContinue) {
    Invoke-Native "curl.exe" @("-I", "--max-time", "20", $SiteUrl) $RepoRoot
}

Remove-Item -LiteralPath $ArchivePath -Force -ErrorAction SilentlyContinue
Remove-Item -LiteralPath $LocalRemoteScript -Force -ErrorAction SilentlyContinue

Write-Host "Done. Uploaded to $SiteUrl" -ForegroundColor Green
