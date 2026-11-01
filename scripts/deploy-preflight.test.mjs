import assert from 'node:assert/strict'
import { randomBytes } from 'node:crypto'
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { afterEach, describe, it } from 'node:test'

const scriptPath = 'scripts/deploy-preflight.ps1'
const source = readFileSync(scriptPath, 'utf8')
const tempRoots = []
const base32Alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'
const knownTotpTestVectors = [
  'GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ',
  'JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP',
  'MZXW6YTBOJQXGZJAMZXXE3DEMF2GK3LQ',
  'NBSWY3DPEB3W64TMMQXG6ZRAMZXXE3DE',
  'MFRGGZDFMZTWQ2LKMFRGGZDFMZTWQ2LK'
]

const toCanonicalBase32 = (bytes) => {
  let buffer = 0
  let bits = 0
  let encoded = ''
  for (const byte of bytes) {
    buffer = (buffer << 8) | byte
    bits += 8
    while (bits >= 5) {
      encoded += base32Alphabet[(buffer >>> (bits - 5)) & 31]
      bits -= 5
      buffer &= (1 << bits) - 1
    }
  }
  if (bits > 0) {
    encoded += base32Alphabet[(buffer << (5 - bits)) & 31]
  }
  return encoded
}

const isSequential = (secret) => {
  let ascending = true
  let descending = true
  for (let index = 1; index < secret.length && (ascending || descending); index += 1) {
    const previous = base32Alphabet.indexOf(secret[index - 1])
    const current = base32Alphabet.indexOf(secret[index])
    ascending &&= current === (previous + 1) % base32Alphabet.length
    descending &&= current === (previous - 1 + base32Alphabet.length) % base32Alphabet.length
  }
  return ascending || descending
}

const isPeriodic = (secret) => {
  for (let period = 1; period <= secret.length / 2; period += 1) {
    if (secret.length % period === 0
      && [...secret].every((symbol, index) => index < period || symbol === secret[index % period])) {
      return true
    }
  }
  return false
}

const generateStrongTotpSecret = () => {
  while (true) {
    // Generated at test runtime so no repository value can be copied as a production credential.
    const candidate = toCanonicalBase32(randomBytes(20))
    if (new Set(candidate).size >= 8
      && !knownTotpTestVectors.includes(candidate)
      && !isSequential(candidate)
      && !isPeriodic(candidate)) {
      return candidate
    }
  }
}

const strongSuperTotpSecret = generateStrongTotpSecret()
let strongAdminTotpSecret = generateStrongTotpSecret()
while (strongAdminTotpSecret === strongSuperTotpSecret) {
  strongAdminTotpSecret = generateStrongTotpSecret()
}

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
    TOTP_REPLAY_FAIL_CLOSED: 'true',
    ANTIBOT_ENABLED: 'true',
    ANTIBOT_LOG_RETENTION_ENABLED: 'true',
    ANTIBOT_LOG_RETENTION_DAYS: '90',
    RECAPTCHA_ENABLED: 'true',
    REGISTRATION_RECAPTCHA_REQUIRED: 'true',
    SCRAPLING_ALLOW_UNAUTHENTICATED: 'false',
    SERVER_FORWARD_HEADERS_STRATEGY: 'none',
    TRUST_PROXY_HEADERS: 'true',
    TRUSTED_PROXY_CIDRS: '172.28.0.10/32',
    FRONTEND_CONTAINER_IP: '172.28.0.10',
    JWT_SECRET: 'a'.repeat(64),
    CSRF_SIGNING_SECRET: 'b'.repeat(64),
    CACHE_KEY_HMAC_SECRET: 'c'.repeat(64),
    METRICS_SCRAPE_TOKEN: 'm'.repeat(64),
    ADMIN_ENCRYPTION_KEY: 'd'.repeat(64),
    PAYMENT_CALLBACK_SECRET: 'e'.repeat(64),
    PII_KEYS: 'k1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=',
    PII_ACTIVE_KID: 'k1',
    DB_PASSWORD: 'db-password-prod',
    MYSQL_ROOT_PASSWORD: 'mysql-root-prod',
    REDIS_PASSWORD: 'redis-password-prod',
    SUPER_ADMIN_TOTP_SECRET: strongSuperTotpSecret,
    SCRAPLING_API_KEY: 'scrapling-prod-key',
    RECAPTCHA_SITE_KEY: 'recaptcha-site-key',
    RECAPTCHA_SECRET_KEY: 'recaptcha-secret-key',
    VITE_AMAP_KEY: 'amap-key',
    VITE_AMAP_SECURITY_CODE: 'amap-security-code',
    GRAFANA_ADMIN_PASSWORD: 'grafana-admin-password',
    ALERTMANAGER_WEBHOOK_URL: 'https://alerts.colorfultibet.cn/webhook',
    APP_VERSION: 'release-2026-07-31',
    NGINX_SERVER_NAME: 'colorfultibet.cn www.colorfultibet.cn',
    NGINX_REDIRECT_HOST: 'colorfultibet.cn',
    NGINX_CERT_DOMAIN: 'colorfultibet.cn',
    DB_SSL_MODE: 'REQUIRED',
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
    const requiredSecrets = [
      'JWT_SECRET',
      'CSRF_SIGNING_SECRET',
      'CACHE_KEY_HMAC_SECRET',
      'METRICS_SCRAPE_TOKEN',
      'ADMIN_ENCRYPTION_KEY',
      'PAYMENT_CALLBACK_SECRET',
      'DB_PASSWORD',
      'MYSQL_ROOT_PASSWORD',
      'REDIS_PASSWORD',
      'SUPER_ADMIN_TOTP_SECRET',
      'SCRAPLING_API_KEY',
      'RECAPTCHA_SITE_KEY',
      'RECAPTCHA_SECRET_KEY',
      'VITE_AMAP_KEY',
      'VITE_AMAP_SECURITY_CODE',
      'GRAFANA_ADMIN_PASSWORD',
      'ALERTMANAGER_WEBHOOK_URL',
      'APP_VERSION'
    ]

    for (const name of requiredSecrets) {
      assert.match(source, new RegExp(`"${name}"`))
    }

    assert.match(source, /Require-RealValue \$EnvValues \$name/)
    assert.match(source, /CACHE_KEY_HMAC_SECRET must be at least 64 characters/)
    assert.match(source, /METRICS_SCRAPE_TOKEN must be at least 64 characters/)
    assert.match(source, /Require-Base32TotpSecret \$EnvValues "SUPER_ADMIN_TOTP_SECRET"/)
    assert.match(source, /must be a high-entropy Base32 secret in complete 8-character blocks/)
    assert.match(source, /-cnotmatch '\^\[A-Z2-7\]\{32,\}\$'/)
    assert.match(source, /Validate-AdminTotpSecrets \$EnvValues/)
    assert.match(source, /ADMIN_TOTP_SECRETS secrets must be independent/)
    for (const knownVector of knownTotpTestVectors) {
      assert.ok(source.includes(`'${knownVector}'`))
    }
  })

  it('keeps production metrics and Redis-backed protections fail-closed', () => {
    assert.match(source, /Require-Exact \$EnvValues "PUBLIC_METRICS_ENABLED" "false"/)
    assert.match(source, /Require-Exact \$EnvValues "RATE_LIMIT_REDIS_ENABLED" "true"/)
    assert.match(source, /Require-Exact \$EnvValues "RATE_LIMIT_REDIS_FAIL_CLOSED" "true"/)
    assert.match(source, /Require-Exact \$EnvValues "BRUTE_FORCE_REDIS_ENABLED" "true"/)
    assert.match(source, /Require-Exact \$EnvValues "BRUTE_FORCE_REDIS_FAIL_CLOSED" "true"/)
    assert.match(source, /Require-Exact \$EnvValues "TOTP_REPLAY_FAIL_CLOSED" "true"/)
    assert.match(source, /Require-Exact \$EnvValues "ANTIBOT_ENABLED" "true"/)
    assert.match(source, /Require-Exact \$EnvValues "ANTIBOT_LOG_RETENTION_ENABLED" "true"/)
    assert.match(source, /Require-IntRange \$EnvValues "ANTIBOT_LOG_RETENTION_DAYS" 1 365/)
    assert.match(source, /Require-Exact \$EnvValues "RECAPTCHA_ENABLED" "true"/)
    assert.match(source, /Require-Exact \$EnvValues "REGISTRATION_RECAPTCHA_REQUIRED" "true"/)
    assert.match(source, /Require-Exact \$EnvValues "SCRAPLING_ALLOW_UNAUTHENTICATED" "false"/)
    assert.match(source, /Require-Exact \$EnvValues "SERVER_FORWARD_HEADERS_STRATEGY" "none"/)
    assert.match(source, /Require-Exact \$EnvValues "TRUST_PROXY_HEADERS" "true"/)
    assert.match(source, /TRUSTED_PROXY_CIDRS must exactly match FRONTEND_CONTAINER_IP\/32/)
  })

  it('rejects disabled production reCAPTCHA in a release preflight run', function () {
    const result = runPreflightWithEnv(baseEnv({
      RECAPTCHA_ENABLED: 'false'
    }))
    if (result.error && result.error.code === 'ENOENT') {
      this.skip('PowerShell is not available in this environment')
      return
    }

    assert.notEqual(result.status, 0)
    assert.match(result.stderr, /RECAPTCHA_ENABLED must be true/)
  })

  it('rejects weak production TOTP secrets in a release preflight run', function () {
    const result = runPreflightWithEnv(baseEnv({
      SUPER_ADMIN_TOTP_SECRET: 'JBSWY3DPEHPK3PXP'
    }))
    if (result.error && result.error.code === 'ENOENT') {
      this.skip('PowerShell is not available in this environment')
      return
    }

    assert.notEqual(result.status, 0)
    assert.match(result.stderr, /SUPER_ADMIN_TOTP_SECRET must be a high-entropy Base32 secret/)
  })

  it('rejects published, patterned, low-diversity, and non-block TOTP secrets without echoing them', function () {
    const rejectedSecrets = [
      ...knownTotpTestVectors,
      'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567',
      'MZXW6YTBOJQXGZJAMZXW6YTBOJQXGZJA',
      'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA',
      `${strongSuperTotpSecret}A`
    ]

    for (const rejectedSecret of rejectedSecrets) {
      const result = runPreflightWithEnv(baseEnv({
        SUPER_ADMIN_TOTP_SECRET: rejectedSecret
      }))
      if (result.error && result.error.code === 'ENOENT') {
        this.skip('PowerShell is not available in this environment')
        return
      }

      assert.notEqual(result.status, 0)
      assert.match(result.stderr, /SUPER_ADMIN_TOTP_SECRET must be a high-entropy Base32 secret/)
      assert.doesNotMatch(result.stderr, new RegExp(rejectedSecret))
    }
  })

  it('rejects lowercase TOTP secrets instead of silently canonicalizing them', function () {
    const lowercaseSecret = strongSuperTotpSecret.toLowerCase()
    const result = runPreflightWithEnv(baseEnv({
      SUPER_ADMIN_TOTP_SECRET: lowercaseSecret
    }))
    if (result.error && result.error.code === 'ENOENT') {
      this.skip('PowerShell is not available in this environment')
      return
    }

    assert.notEqual(result.status, 0)
    assert.match(result.stderr, /SUPER_ADMIN_TOTP_SECRET must be a high-entropy Base32 secret/)
    assert.doesNotMatch(result.stderr, new RegExp(lowercaseSecret))
  })

  it('accepts an optional valid administrator TOTP map', function () {
    const result = runPreflightWithEnv(baseEnv({
      ADMIN_TOTP_SECRETS: `admin=${strongAdminTotpSecret}`
    }))
    if (result.error && result.error.code === 'ENOENT') {
      this.skip('PowerShell is not available in this environment')
      return
    }

    assert.equal(result.status, 0, result.stderr || result.stdout)
    assert.match(result.stdout, /Deployment preflight passed for release/)
  })

  it('rejects malformed or reused administrator TOTP map entries', function () {
    const result = runPreflightWithEnv(baseEnv({
      ADMIN_TOTP_SECRETS: `admin=replace-with-another-base32-secret;ADMIN=${strongSuperTotpSecret}`
    }))
    if (result.error && result.error.code === 'ENOENT') {
      this.skip('PowerShell is not available in this environment')
      return
    }

    assert.notEqual(result.status, 0)
    assert.match(result.stderr, /ADMIN_TOTP_SECRETS contains an empty or placeholder secret|duplicate or super-admin username/)
    assert.doesNotMatch(result.stderr, /replace-with-another-base32-secret/)
  })

  it('rejects weak administrator map secrets without echoing them', function () {
    const sentinel = 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'
    const result = runPreflightWithEnv(baseEnv({
      ADMIN_TOTP_SECRETS: `admin=${sentinel}`
    }))
    if (result.error && result.error.code === 'ENOENT') {
      this.skip('PowerShell is not available in this environment')
      return
    }

    assert.notEqual(result.status, 0)
    assert.match(result.stderr, /ADMIN_TOTP_SECRETS secret must be a high-entropy Base32 secret/)
    assert.doesNotMatch(result.stderr, new RegExp(sentinel))
  })

  it('never echoes placeholder secrets or malformed environment lines', function () {
    const placeholderSentinel = 'replace-with-LEAK-CANARY-TOTP-SECRET'
    const placeholderResult = runPreflightWithEnv(baseEnv({
      SUPER_ADMIN_TOTP_SECRET: placeholderSentinel
    }))
    if (placeholderResult.error && placeholderResult.error.code === 'ENOENT') {
      this.skip('PowerShell is not available in this environment')
      return
    }

    assert.notEqual(placeholderResult.status, 0)
    assert.doesNotMatch(placeholderResult.stderr, /LEAK-CANARY-TOTP-SECRET/)

    const malformedSentinel = 'MALFORMED-LEAK-CANARY-SECRET'
    const malformedResult = runPreflightWithEnv(`${baseEnv()}\nADMIN_TOTP_SECRETS ${malformedSentinel}`)
    assert.notEqual(malformedResult.status, 0)
    assert.match(malformedResult.stderr, /line contents were suppressed/)
    assert.doesNotMatch(malformedResult.stderr, new RegExp(malformedSentinel))
  })

  it('never echoes malformed PII key material', function () {
    const piiKeySentinel = 'LEAK-CANARY-PII-KEY-MATERIAL'
    const result = runPreflightWithEnv(baseEnv({
      PII_KEYS: piiKeySentinel
    }))
    if (result.error && result.error.code === 'ENOENT') {
      this.skip('PowerShell is not available in this environment')
      return
    }

    assert.notEqual(result.status, 0)
    assert.match(result.stderr, /PII_KEYS contains an entry.*entry contents were suppressed/)
    assert.doesNotMatch(result.stderr, new RegExp(piiKeySentinel))
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

  it('rejects process-local TOTP replay fallback in a release preflight run', function () {
    const result = runPreflightWithEnv(baseEnv({
      TOTP_REPLAY_FAIL_CLOSED: 'false'
    }))
    if (result.error && result.error.code === 'ENOENT') {
      this.skip('PowerShell is not available in this environment')
      return
    }

    assert.notEqual(result.status, 0)
    assert.match(result.stderr, /TOTP_REPLAY_FAIL_CLOSED must be true/)
  })

  it('rejects unbounded anti-bot log retention in a release preflight run', function () {
    const result = runPreflightWithEnv(baseEnv({
      ANTIBOT_LOG_RETENTION_DAYS: '0'
    }))
    if (result.error && result.error.code === 'ENOENT') {
      this.skip('PowerShell is not available in this environment')
      return
    }

    assert.notEqual(result.status, 0)
    assert.match(result.stderr, /ANTIBOT_LOG_RETENTION_DAYS must be an integer between 1 and 365/)
  })

  it('rejects Spring forwarding-header overrides in a release preflight run', function () {
    const result = runPreflightWithEnv(baseEnv({
      SERVER_FORWARD_HEADERS_STRATEGY: 'framework'
    }))
    if (result.error && result.error.code === 'ENOENT') {
      this.skip('PowerShell is not available in this environment')
      return
    }

    assert.notEqual(result.status, 0)
    assert.match(result.stderr, /SERVER_FORWARD_HEADERS_STRATEGY must be none/)
  })

  it('rejects trusted proxy CIDRs that drift from the pinned nginx address', function () {
    const result = runPreflightWithEnv(baseEnv({
      TRUSTED_PROXY_CIDRS: '172.28.0.11/32'
    }))
    if (result.error && result.error.code === 'ENOENT') {
      this.skip('PowerShell is not available in this environment')
      return
    }

    assert.notEqual(result.status, 0)
    assert.match(result.stderr, /TRUSTED_PROXY_CIDRS must exactly match FRONTEND_CONTAINER_IP\/32/)
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
