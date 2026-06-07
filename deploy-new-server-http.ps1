param(
    [string]$HostName = "154.202.118.208",
    [string]$User = "root",
    [string]$RemoteProjectDir = "/opt/colorful-tibet",
    [string]$SiteUrl = "",
    [string]$ArchivePath = (Join-Path $env:TEMP "colorful-tibet-http-upload.tar.gz"),
    [securestring]$Password,
    [switch]$SkipLocalChecks,
    [switch]$RegenerateRemoteEnv,
    [switch]$DemoOnly,
    [string]$DoubaoApiKey = "",
    [string]$AmapKey = "",
    [string]$AmapSecurityCode = ""
)

$ErrorActionPreference = "Stop"

$scriptName = Split-Path -Leaf $MyInvocation.MyCommand.Path
if (-not $DemoOnly) {
    throw "$scriptName generates an HTTP local-profile demo deployment with mock payment callbacks. Re-run with -DemoOnly to acknowledge this, or use docker-compose.prod.yml for production."
}
Write-Warning "$scriptName is demo-only: it deploys over HTTP with SPRING_PROFILES_ACTIVE=local, REQUIRE_STRONG_SECRETS=false, and PAYMENT_MOCK_CALLBACK_ENABLED=true."

$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $SiteUrl) {
    $SiteUrl = "http://$HostName/"
}

$RemoteArchive = "/tmp/colorful-tibet-http-upload.tar.gz"
$RemoteEnv = "/tmp/colorful-tibet-http.env"
$RemoteLogin = "/tmp/colorful-tibet-first-login.txt"
$RemoteScript = "/tmp/colorful-tibet-http-deploy.sh"

$LocalEnv = Join-Path $env:TEMP "colorful-tibet-http.env"
$LocalLogin = Join-Path $env:TEMP "colorful-tibet-first-login.txt"
$LocalRemoteScript = Join-Path $env:TEMP "colorful-tibet-http-deploy.sh"
$LocalUploader = Join-Path $env:TEMP "colorful-tibet-paramiko-upload.py"

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

function ConvertTo-PlainText {
    param([securestring]$SecureValue)
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    }
    finally {
        if ($bstr -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
        }
    }
}

function New-SecretSet {
    $generator = @'
import base64
import json
import secrets

def b64(byte_count):
    return base64.b64encode(secrets.token_bytes(byte_count)).decode("ascii")

def b32(byte_count):
    return base64.b32encode(secrets.token_bytes(byte_count)).decode("ascii").rstrip("=")

def token(byte_count=24):
    return secrets.token_urlsafe(byte_count)

print(json.dumps({
    "mysqlRootPassword": token(30),
    "mysqlPassword": token(30),
    "jwtSecret": b64(64),
    "csrfSigningSecret": b64(64),
    "adminEncryptionKey": b64(48),
    "piiEncryptionKey": b64(48),
    "piiKey": b64(32),
    "paymentCallbackSecret": b64(48),
    "superAdminTotpSecret": b32(20),
    "seedAdminPassword": token(24),
    "seedSuperAdminPassword": token(24),
    "seedUserPassword": token(24),
    "grafanaAdminPassword": token(24)
}))
'@
    $json = $generator | python -
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to generate deployment secrets"
    }
    return $json | ConvertFrom-Json
}

function Write-DeploymentEnv {
    param([object]$Secrets)

    $corsOrigin = "http://$HostName"
    $envContent = @"
SPRING_PROFILES_ACTIVE=local
VITE_API_BASE_URL=/api

MYSQL_DATABASE=colorful_tibet
MYSQL_USERNAME=colorful_tibet_app
MYSQL_PASSWORD=$($Secrets.mysqlPassword)
MYSQL_ROOT_PASSWORD=$($Secrets.mysqlRootPassword)
MYSQL_HOST=mysql
MYSQL_PORT=3306
MYSQL_SSL_MODE=DISABLED
MYSQL_ALLOW_PUBLIC_KEY_RETRIEVAL=true

DB_NAME=tibet_tourism
DB_USERNAME=tibet_user
DB_PASSWORD=$($Secrets.mysqlPassword)
DB_HOST=mysql
DB_PORT=3306
DB_SSL_MODE=DISABLED
DB_ALLOW_PUBLIC_KEY_RETRIEVAL=true

REDIS_HOST=redis
REDIS_PORT=6379
REDIS_DATABASE=0

JWT_SECRET=$($Secrets.jwtSecret)
ADMIN_ENCRYPTION_KEY=$($Secrets.adminEncryptionKey)
CSRF_SIGNING_SECRET=$($Secrets.csrfSigningSecret)
PII_ACTIVE_KID=v1
PII_KEYS=v1:$($Secrets.piiKey)
PII_ENCRYPTION_KEY=$($Secrets.piiEncryptionKey)
PII_MIGRATION_ENABLED=false
PAYMENT_CALLBACK_SECRET=$($Secrets.paymentCallbackSecret)
REQUIRE_STRONG_SECRETS=false

COOKIE_SECURE=false
TRUST_PROXY_HEADERS=true
TRUSTED_PROXY_CIDRS=172.16.0.0/12,10.0.0.0/8,127.0.0.1/32
CORS_ALLOWED_ORIGINS=$corsOrigin
PUBLIC_DOCS_ENABLED=false
SEED_CONTENT_ENABLED=true
SEED_DEMO_USERS=false
SUPER_ADMIN_USERNAME=lzh
SUPER_ADMIN_TOTP_SECRET=$($Secrets.superAdminTotpSecret)
SEED_DEMO_ADMIN_PASSWORD=$($Secrets.seedAdminPassword)
SEED_DEMO_SUPER_ADMIN_PASSWORD=$($Secrets.seedSuperAdminPassword)
SEED_DEMO_USER_PASSWORD=$($Secrets.seedUserPassword)

DOUBAO_API_KEY=$DoubaoApiKey
DOUBAO_API_URL=https://ark.cn-beijing.volces.com/api/v3/responses
DOUBAO_STREAM_API_URL=
DOUBAO_MODEL=ep-20260516173036-4dpgm
ARK_API_KEY=$DoubaoApiKey
ARK_API_URL=https://ark.cn-beijing.volces.com/api/v3/responses
ARK_STREAM_API_URL=
ARK_MODEL=ep-20260516173036-4dpgm
AI_MODEL=ep-20260516173036-4dpgm
AI_STREAM_TIMEOUT=180
AI_DAILY_QUOTA_PER_USER=20
GUIDE_CHAT_ANONYMOUS_ENABLED=true
GUIDE_CHAT_ANONYMOUS_DAILY_QUOTA_PER_IP=5
GUIDE_CHAT_AUTHENTICATED_DAILY_QUOTA_PER_USER=30
GUIDE_CHAT_REQUIRE_RECAPTCHA_AFTER=2
GUIDE_CHAT_ANON_REMOTE_AI_ENABLED=false

ANTIBOT_ENABLED=false
RECAPTCHA_ENABLED=false
VITE_RECAPTCHA_ENABLED=false
VITE_RECAPTCHA_MODE=v2
RECAPTCHA_SITE_KEY=
VITE_RECAPTCHA_SITE_KEY=
RECAPTCHA_SECRET_KEY=
FINGERPRINT_ENABLED=true
BEHAVIOR_ENABLED=true

VITE_AMAP_KEY=$AmapKey
VITE_AMAP_SECURITY_CODE=$AmapSecurityCode
IP_LOCATION_URL_TEMPLATE=https://ipapi.co/{ip}/json/
SCRAPLING_SERVICE_URL=http://scrapling:8000
SCRAPLING_SERVICE_TIMEOUT=30
SCRAPLING_HEALTH_ENABLED=true
SCRAPLING_MODE=basic
SCRAPLING_TIMEOUT_SECONDS=30
SCRAPLING_RETRIES=2
SCRAPLING_MAX_SOURCES=4
SCRAPLING_SEARCH_PROVIDERS=baidu,bing
SCRAPLING_ALLOWED_DOMAINS=
SCRAPLING_PROXY_URL=
SCRAPLING_SOLVE_CLOUDFLARE=false

FILE_UPLOAD_DIR=/app/data/uploads
NGINX_SERVER_NAME=$HostName
NGINX_CERT_DOMAIN=$HostName
FRONTEND_HOST_PORT=80
BACKEND_HOST_PORT=8080

ALERTMANAGER_WEBHOOK_URL=
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=$($Secrets.grafanaAdminPassword)
ZIPKIN_ENDPOINT=http://zipkin:9411/api/v2/spans
TRACING_SAMPLING_PROBABILITY=0.0

PAYMENT_MOCK_CALLBACK_ENABLED=true
RECOMMENDATION_ITEM_SIMILARITY_PRELOAD_ON_STARTUP=false
APP_RECOMMENDATION_ITEM_SIMILARITY_MAX_SPOTS=2000
APP_RECOMMENDATION_ITEM_SIMILARITY_MAX_HISTORIES=50000
APP_PRICE_UPDATE_MAX_BATCH_SPOTS=1000
"@

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($LocalEnv, ($envContent -replace "`r`n", "`n"), $utf8NoBom)

    $loginContent = @"
Colorful Tibet demo-only first login details
Generated by deploy-new-server-http.ps1 -DemoOnly.

Site: $SiteUrl

Super admin username: lzh
Super admin password: $($Secrets.seedSuperAdminPassword)
Super admin TOTP secret: $($Secrets.superAdminTotpSecret)
TOTP URI: otpauth://totp/ColorfulTibet:lzh?secret=$($Secrets.superAdminTotpSecret)&issuer=ColorfulTibet

Admin username: admin
Admin password: $($Secrets.seedAdminPassword)

Demo user username: user1
Demo user password: $($Secrets.seedUserPassword)

This HTTP deployment is for demos only. These seed passwords are only used when the database is empty. Change them after first login.
"@
    [System.IO.File]::WriteAllText($LocalLogin, ($loginContent -replace "`r`n", "`n"), $utf8NoBom)
}

function Write-RemoteScript {
    $projectDirLiteral = ConvertTo-ShellSingleQuoted $RemoteProjectDir
    $archiveLiteral = ConvertTo-ShellSingleQuoted $RemoteArchive
    $envLiteral = ConvertTo-ShellSingleQuoted $RemoteEnv
    $loginLiteral = ConvertTo-ShellSingleQuoted $RemoteLogin
    $siteUrlLiteral = ConvertTo-ShellSingleQuoted $SiteUrl
    $regenerateValue = if ($RegenerateRemoteEnv) { "true" } else { "false" }

    $remoteScriptContent = @'
#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR=__PROJECT_DIR__
ARCHIVE=__ARCHIVE__
ENV_UPLOAD=__ENV_UPLOAD__
LOGIN_UPLOAD=__LOGIN_UPLOAD__
SITE_URL=__SITE_URL__
REGENERATE_ENV=__REGENERATE_ENV__
COMPOSE_FILE="$PROJECT_DIR/docker-compose.yml"

case "$PROJECT_DIR" in
  /opt/colorful-tibet|/opt/colorful-tibet/*) ;;
  *) echo "Refusing unexpected project path: $PROJECT_DIR" >&2; exit 1 ;;
esac

install_basic_tools() {
  if command -v apt-get >/dev/null 2>&1; then
    apt-get update
    apt-get install -y ca-certificates curl tar gzip
  elif command -v dnf >/dev/null 2>&1; then
    dnf install -y ca-certificates curl tar gzip
  elif command -v yum >/dev/null 2>&1; then
    yum install -y ca-certificates curl tar gzip
  fi
}

install_docker_if_needed() {
  if ! command -v docker >/dev/null 2>&1; then
    install_basic_tools
    curl -fsSL https://get.docker.com -o /tmp/get-docker.sh
    sh /tmp/get-docker.sh
  fi

  systemctl enable --now docker >/dev/null 2>&1 || service docker start >/dev/null 2>&1 || true

  if ! docker compose version >/dev/null 2>&1; then
    if command -v apt-get >/dev/null 2>&1; then
      apt-get update
      apt-get install -y docker-compose-plugin
    elif command -v dnf >/dev/null 2>&1; then
      dnf install -y docker-compose-plugin
    elif command -v yum >/dev/null 2>&1; then
      yum install -y docker-compose-plugin
    fi
  fi

  docker --version
  docker compose version
}

open_host_firewall() {
  if command -v ufw >/dev/null 2>&1 && ufw status | grep -qi active; then
    ufw allow OpenSSH >/dev/null 2>&1 || true
    ufw allow 80/tcp >/dev/null 2>&1 || true
  fi

  if command -v firewall-cmd >/dev/null 2>&1 && systemctl is-active --quiet firewalld; then
    firewall-cmd --permanent --add-service=ssh >/dev/null 2>&1 || true
    firewall-cmd --permanent --add-service=http >/dev/null 2>&1 || true
    firewall-cmd --reload >/dev/null 2>&1 || true
  fi
}

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
  docker compose -f "$COMPOSE_FILE" logs --tail=180 backend frontend mysql redis scrapling >&2 || true
  return 1
}

install_docker_if_needed
open_host_firewall

mkdir -p "$PROJECT_DIR" "$PROJECT_DIR/data" "$PROJECT_DIR/logs"

if [ -f "$COMPOSE_FILE" ]; then
  echo "Stopping existing containers..."
  (cd "$PROJECT_DIR" && docker compose -f "$COMPOSE_FILE" down --remove-orphans) || true
fi

echo "Replacing application files..."
find "$PROJECT_DIR" -mindepth 1 -maxdepth 1 \
  ! -name '.env' \
  ! -name 'data' \
  ! -name 'logs' \
  ! -name 'certs' \
  -exec rm -rf -- {} +

tar -xzf "$ARCHIVE" -C "$PROJECT_DIR"

if [ ! -f "$PROJECT_DIR/.env" ] || [ "$REGENERATE_ENV" = "true" ]; then
  cp "$ENV_UPLOAD" "$PROJECT_DIR/.env"
  chmod 600 "$PROJECT_DIR/.env"
  cp "$LOGIN_UPLOAD" /root/colorful-tibet-first-login.txt
  chmod 600 /root/colorful-tibet-first-login.txt
  echo "Created $PROJECT_DIR/.env and /root/colorful-tibet-first-login.txt"
else
  echo "Keeping existing $PROJECT_DIR/.env"
fi

mkdir -p "$PROJECT_DIR/data/uploads" "$PROJECT_DIR/logs"
if [ -d "$PROJECT_DIR/backend/uploads" ]; then
  cp -an "$PROJECT_DIR/backend/uploads/." "$PROJECT_DIR/data/uploads/" || true
fi
chmod -R a+rwX "$PROJECT_DIR/data" "$PROJECT_DIR/logs"

cd "$PROJECT_DIR"
docker compose -f "$COMPOSE_FILE" config --quiet

echo "Building and starting HTTP deployment..."
COMPOSE_PROGRESS=plain BUILDKIT_PROGRESS=plain docker compose -f "$COMPOSE_FILE" up -d --build

wait_for_health colorful-tibet-mysql 240
wait_for_health colorful-tibet-redis 120
wait_for_health colorful-tibet-scrapling 240
wait_for_health colorful-tibet-backend 600
wait_for_health colorful-tibet-frontend 240

docker compose -f "$COMPOSE_FILE" ps
curl -fsS http://127.0.0.1:8080/actuator/health/readiness
echo ""
curl -fsS http://127.0.0.1/health
curl -fsSI --max-time 20 "$SITE_URL" | head -n 12 || echo "WARNING: public URL check failed from the server; check cloud firewall/security group for TCP 80."

rm -f "$ARCHIVE" "$ENV_UPLOAD" "$LOGIN_UPLOAD" /tmp/colorful-tibet-http-deploy.sh /tmp/get-docker.sh
echo "HTTP demo deployment completed."
'@

    $remoteScriptContent = $remoteScriptContent.Replace("__PROJECT_DIR__", $projectDirLiteral)
    $remoteScriptContent = $remoteScriptContent.Replace("__ARCHIVE__", $archiveLiteral)
    $remoteScriptContent = $remoteScriptContent.Replace("__ENV_UPLOAD__", $envLiteral)
    $remoteScriptContent = $remoteScriptContent.Replace("__LOGIN_UPLOAD__", $loginLiteral)
    $remoteScriptContent = $remoteScriptContent.Replace("__SITE_URL__", $siteUrlLiteral)
    $remoteScriptContent = $remoteScriptContent.Replace("__REGENERATE_ENV__", $regenerateValue)
    $remoteScriptContent = $remoteScriptContent -replace "`r`n", "`n"

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($LocalRemoteScript, $remoteScriptContent, $utf8NoBom)
}

function Write-Uploader {
    $uploader = @'
import argparse
import os
import select
import sys
import time

try:
    import paramiko
except Exception as exc:
    print(f"Missing Python dependency paramiko: {exc}", file=sys.stderr)
    print("Install it with: python -m pip install paramiko", file=sys.stderr)
    sys.exit(2)

parser = argparse.ArgumentParser()
parser.add_argument("--host", required=True)
parser.add_argument("--user", required=True)
parser.add_argument("--archive", required=True)
parser.add_argument("--env-file", required=True)
parser.add_argument("--login-file", required=True)
parser.add_argument("--script-file", required=True)
parser.add_argument("--remote-archive", required=True)
parser.add_argument("--remote-env", required=True)
parser.add_argument("--remote-login", required=True)
parser.add_argument("--remote-script", required=True)
args = parser.parse_args()

password = os.environ.get("COLORFUL_TIBET_DEPLOY_PASSWORD")
if not password:
    print("Missing COLORFUL_TIBET_DEPLOY_PASSWORD", file=sys.stderr)
    sys.exit(2)

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(
    hostname=args.host,
    username=args.user,
    password=password,
    timeout=30,
    banner_timeout=30,
    auth_timeout=30,
    look_for_keys=False,
    allow_agent=False,
)

try:
    sftp = client.open_sftp()
    try:
        for local_path, remote_path in [
            (args.archive, args.remote_archive),
            (args.env_file, args.remote_env),
            (args.login_file, args.remote_login),
            (args.script_file, args.remote_script),
        ]:
            print(f"Uploading {local_path} -> {remote_path}", flush=True)
            sftp.put(local_path, remote_path)
    finally:
        sftp.close()

    command = f"chmod 700 {args.remote_script} && bash {args.remote_script}"
    stdin, stdout, stderr = client.exec_command(command, get_pty=False)
    channel = stdout.channel

    while not channel.exit_status_ready():
        if channel.recv_ready():
            sys.stdout.write(channel.recv(65535).decode("utf-8", "replace"))
            sys.stdout.flush()
        if channel.recv_stderr_ready():
            sys.stderr.write(channel.recv_stderr(65535).decode("utf-8", "replace"))
            sys.stderr.flush()
        time.sleep(0.2)

    while channel.recv_ready():
        sys.stdout.write(channel.recv(65535).decode("utf-8", "replace"))
    while channel.recv_stderr_ready():
        sys.stderr.write(channel.recv_stderr(65535).decode("utf-8", "replace"))
    sys.stdout.flush()
    sys.stderr.flush()

    code = channel.recv_exit_status()
    if code != 0:
        print(f"Remote deployment failed with exit code {code}", file=sys.stderr)
    sys.exit(code)
finally:
    client.close()
'@

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($LocalUploader, ($uploader -replace "`r`n", "`n"), $utf8NoBom)
}

Assert-Command "tar"
Assert-Command "python"

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

if (-not $Password) {
    $Password = Read-Host "SSH password for $User@$HostName" -AsSecureString
}
$plainPassword = ConvertTo-PlainText $Password

try {
    if (Test-Path $ArchivePath) {
        Remove-Item -LiteralPath $ArchivePath -Force
    }

    $tarExcludes = @(
        "--exclude=./.git",
        "--exclude=./.idea",
        "--exclude=./.vscode",
        "--exclude=./.claude",
        "--exclude=./.cursor",
        "--exclude=./node_modules",
        "--exclude=./frontend/node_modules",
        "--exclude=./frontend/dist",
        "--exclude=./backend/target",
        "--exclude=./backend/logs",
        "--exclude=./logs",
        "--exclude=./output",
        "--exclude=./.startup",
        "--exclude=./certs",
        "--exclude=./scrapler/.venv",
        "--exclude=./scrapler/__pycache__",
        "--exclude=./.env",
        "--exclude=./.env.*",
        "--exclude=*/.env",
        "--exclude=*/.env.*",
        "--exclude=./*.log",
        "--exclude=./*.tar.gz"
    )

    $topLevelExcludes = @(".git", ".idea", ".vscode", ".claude", ".cursor", "node_modules", "logs", "output", ".env", ".startup", "certs")
    $tarIncludes = Get-ChildItem -LiteralPath $RepoRoot -Force |
        Where-Object {
            $topLevelExcludes -notcontains $_.Name -and
            $_.Name -notlike "*.tar.gz"
        } |
        ForEach-Object { "./$($_.Name)" }

    if (-not $tarIncludes) {
        throw "No files found to upload."
    }

    Write-Host "Generating first-deploy environment..." -ForegroundColor Cyan
    $secrets = New-SecretSet
    Write-DeploymentEnv $secrets
    Write-RemoteScript
    Write-Uploader

    Write-Host "Packing project..." -ForegroundColor Cyan
    Invoke-Native "tar" ($tarExcludes + @("-czf", $ArchivePath) + $tarIncludes) $RepoRoot

    $env:COLORFUL_TIBET_DEPLOY_PASSWORD = $plainPassword
    Write-Host "Uploading and deploying to $User@$HostName..." -ForegroundColor Cyan
    Invoke-Native "python" @(
        $LocalUploader,
        "--host", $HostName,
        "--user", $User,
        "--archive", $ArchivePath,
        "--env-file", $LocalEnv,
        "--login-file", $LocalLogin,
        "--script-file", $LocalRemoteScript,
        "--remote-archive", $RemoteArchive,
        "--remote-env", $RemoteEnv,
        "--remote-login", $RemoteLogin,
        "--remote-script", $RemoteScript
    ) $RepoRoot

    Write-Host "Done. Site: $SiteUrl" -ForegroundColor Green
    Write-Host "First login details are on the server at /root/colorful-tibet-first-login.txt" -ForegroundColor Green
}
finally {
    $env:COLORFUL_TIBET_DEPLOY_PASSWORD = $null
    foreach ($path in @($ArchivePath, $LocalEnv, $LocalLogin, $LocalRemoteScript, $LocalUploader)) {
        Remove-Item -LiteralPath $path -Force -ErrorAction SilentlyContinue
    }
}
