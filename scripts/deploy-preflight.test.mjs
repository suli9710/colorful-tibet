import assert from 'node:assert/strict'
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { afterEach, describe, it } from 'node:test'

const scriptPath = 'scripts/deploy-preflight.ps1'
const source = readFileSync(scriptPath, 'utf8')
const tempRoots = []

const runPowerShellParser = () => {
  const command = [
    '$ErrorActionPreference = "Stop"',
    `$source = Get-Content -LiteralPath '${scriptPath}' -Raw`,
    '$tokens = $null',
    '$errors = $null',
    '[System.Management.Automation.Language.Parser]::ParseInput($source, [ref] $tokens, [ref] $errors) | Out-Null',
    'if ($errors.Count -gt 0) { $errors | ForEach-Object { Write-Error $_.Message }; exit 1 }'
  ].join('; ')

  return spawnSync('powershell', ['-NoProfile', '-NonInteractive', '-Command', command], {
    encoding: 'utf8',
    timeout: 60000
  })
}

const baseEnv = (overrides = {}) => {
  const values = {
    SPRING_PROFILES_ACTIVE: 'prod',
    REQUIRE_STRONG_SECRETS: 'true',
    COOKIE_SECURE: 'true',
    PAYMENT_MOCK_CALLBACK_ENABLED: 'false',
    SEED_DEMO_USERS: 'false',
    SEED_CONTENT_ENABLED: 'false',
    SPRING_FLYWAY_ENABLED: 'true',
    SPRING_JPA_HIBERNATE_DDL_AUTO: 'validate',
    DB_ALLOW_PUBLIC_KEY_RETRIEVAL: 'false',
    PUBLIC_METRICS_ENABLED: 'false',
    RATE_LIMIT_REDIS_ENABLED: 'true',
    RATE_LIMIT_REDIS_FAIL_CLOSED: 'true',
    BRUTE_FORCE_REDIS_ENABLED: 'true',
    BRUTE_FORCE_REDIS_FAIL_CLOSED: 'true',
    SCRAPLING_ALLOW_UNAUTHENTICATED: 'false',
    JWT_SECRET: 'a'.repeat(64),
    CSRF_SIGNING_SECRET: 'b'.repeat(64),
    CACHE_KEY_HMAC_SECRET: 'c'.repeat(64),
    ADMIN_ENCRYPTION_KEY: 'd'.repeat(64),
    PAYMENT_CALLBACK_SECRET: 'e'.repeat(64),
    DB_PASSWORD: 'db-password-prod',
    MYSQL_ROOT_PASSWORD: 'mysql-root-prod',
    REDIS_PASSWORD: 'redis-password-prod',
    SUPER_ADMIN_TOTP_SECRET: 'JBSWY3DPEHPK3PXP',
    SCRAPLING_API_KEY: 'scrapling-prod-key',
    RECAPTCHA_SITE_KEY: 'recaptcha-site-key',
    RECAPTCHA_SECRET_KEY: 'recaptcha-secret-key',
    VITE_AMAP_KEY: 'amap-key',
    VITE_AMAP_SECURITY_CODE: 'amap-security-code',
    NGINX_SERVER_NAME: 'colorfultibet.cn www.colorfultibet.cn',
    NGINX_REDIRECT_HOST: 'colorfultibet.cn',
    NGINX_CERT_DOMAIN: 'colorfultibet.cn',
    DB_SSL_MODE: 'REQUIRED',
    PII_KEYS: 'k1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=',
    PII_ACTIVE_KID: 'k1',
    PII_MIGRATION_ENABLED: 'false',
    ...overrides
  }

  return Object.entries(values)
    .map(([key, value]) => `${key}=${value}`)
    .join('\n')
}

const runPreflightWithEnv = (envSource) => {
  const root = mkdtempSync(path.join(tmpdir(), 'ct-preflight-'))
  tempRoots.push(root)
  const envPath = path.join(root, '.env')
  writeFileSync(envPath, envSource)

  return spawnSync('powershell', [
    '-NoProfile',
    '-NonInteractive',
    '-ExecutionPolicy',
    'Bypass',
    '-File',
    scriptPath,
    '-Stage',
    'release',
    '-EnvFile',
    envPath,
    '-SkipDigestEvidence',
    '-SkipComposeConfig'
  ], {
    encoding: 'utf8',
    timeout: 60000
  })
}

afterEach(() => {
  for (const root of tempRoots) {
    rmSync(root, { recursive: true, force: true })
  }
  tempRoots.length = 0
})

describe('deploy preflight script', () => {
  it('is valid PowerShell syntax when PowerShell is available', function () {
    const result = runPowerShellParser()
    if (result.error && result.error.code === 'ENOENT') {
      this.skip('PowerShell is not available in this environment')
      return
    }

    assert.equal(result.status, 0, result.stderr || result.stdout)
  })

  it('keeps deployment checks offline and non-destructive', () => {
    assert.match(source, /check-supply-chain-pins\.mjs/)
    assert.match(source, /--evidence-only/)
    assert.match(source, /do not invent digests offline/i)
    assert.doesNotMatch(source, /\bdocker\s+compose\s+(?:up|down|restart|rm|pull|push)\b/i)
    assert.doesNotMatch(source, /\bRemove-Item\b/i)
  })

  it('requires an explicit PII backfill override and admin audit migration check', () => {
    assert.match(source, /PII_MIGRATION_ENABLED=true is blocked by default/)
    assert.match(source, /AllowPiiBackfill/)
    assert.match(source, /PiiBackfillTicket/)
    assert.match(source, /V24__create_admin_audit_logs\.sql/)
    assert.match(source, /SPRING_FLYWAY_ENABLED/)
    assert.match(source, /SPRING_JPA_HIBERNATE_DDL_AUTO/)
  })

  it('requires an independent cache key HMAC secret before production deploy', () => {
    assert.match(source, /CACHE_KEY_HMAC_SECRET/)
    assert.match(source, /Require-RealValue \$EnvValues \$name/)
    assert.match(source, /CACHE_KEY_HMAC_SECRET must be at least 64 characters/)
  })

  it('keeps production metrics and Redis-backed protections fail-closed', () => {
    assert.match(source, /Require-Exact \$EnvValues "PUBLIC_METRICS_ENABLED" "false"/)
    assert.match(source, /Require-Exact \$EnvValues "RATE_LIMIT_REDIS_ENABLED" "true"/)
    assert.match(source, /Require-Exact \$EnvValues "RATE_LIMIT_REDIS_FAIL_CLOSED" "true"/)
    assert.match(source, /Require-Exact \$EnvValues "BRUTE_FORCE_REDIS_ENABLED" "true"/)
    assert.match(source, /Require-Exact \$EnvValues "BRUTE_FORCE_REDIS_FAIL_CLOSED" "true"/)
    assert.match(source, /Require-Exact \$EnvValues "SCRAPLING_ALLOW_UNAUTHENTICATED" "false"/)
  })

  it('rejects anonymous production metrics in a release preflight run', function () {
    const result = runPreflightWithEnv(baseEnv({
      PUBLIC_METRICS_ENABLED: 'true'
    }))
    if (result.error && result.error.code === 'ENOENT') {
      this.skip('PowerShell is not available in this environment')
      return
    }

    assert.notEqual(result.status, 0)
    assert.match(result.stderr, /PUBLIC_METRICS_ENABLED must be false/)
  })

  it('rejects Redis brute-force fallback in a release preflight run', function () {
    const result = runPreflightWithEnv(baseEnv({
      BRUTE_FORCE_REDIS_FAIL_CLOSED: 'false'
    }))
    if (result.error && result.error.code === 'ENOENT') {
      this.skip('PowerShell is not available in this environment')
      return
    }

    assert.notEqual(result.status, 0)
    assert.match(result.stderr, /BRUTE_FORCE_REDIS_FAIL_CLOSED must be true/)
  })

  it('rejects unauthenticated production scrapling access in a release preflight run', function () {
    const result = runPreflightWithEnv(baseEnv({
      SCRAPLING_ALLOW_UNAUTHENTICATED: 'true'
    }))
    if (result.error && result.error.code === 'ENOENT') {
      this.skip('PowerShell is not available in this environment')
      return
    }

    assert.notEqual(result.status, 0)
    assert.match(result.stderr, /SCRAPLING_ALLOW_UNAUTHENTICATED must be false/)
  })

  it('validates nginx host interpolation values before production deploy', () => {
    assert.match(source, /Require-ServerNameList \$EnvValues "NGINX_SERVER_NAME"/)
    assert.match(source, /Require-SingleHost \$EnvValues "NGINX_REDIRECT_HOST"/)
    assert.match(source, /Require-SingleHost \$EnvValues "NGINX_CERT_DOMAIN"/)
    assert.match(source, /entries must be DNS hosts separated by single spaces only/)
    assert.match(source, /without scheme, path, port, comma, whitespace, or control characters/)
  })

  it('accepts valid nginx host settings in a release preflight run', function () {
    const result = runPreflightWithEnv(baseEnv())
    if (result.error && result.error.code === 'ENOENT') {
      this.skip('PowerShell is not available in this environment')
      return
    }

    assert.equal(result.status, 0, result.stderr || result.stdout)
    assert.match(result.stdout, /Deployment preflight passed for release/)
  })

  it('rejects nginx redirect hosts with schemes in a release preflight run', function () {
    const result = runPreflightWithEnv(baseEnv({
      NGINX_REDIRECT_HOST: 'https://colorfultibet.cn'
    }))
    if (result.error && result.error.code === 'ENOENT') {
      this.skip('PowerShell is not available in this environment')
      return
    }

    assert.notEqual(result.status, 0)
    assert.match(result.stderr, /NGINX_REDIRECT_HOST must be a single DNS host/)
  })
})
