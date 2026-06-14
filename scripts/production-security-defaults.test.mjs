import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { describe, it } from 'node:test'

const prodCompose = readFileSync('docker-compose.prod.yml', 'utf8').replace(/\r\n/g, '\n')
const envExample = readFileSync('.env.example', 'utf8').replace(/\r\n/g, '\n')
const prometheusConfig = readFileSync('monitoring/prometheus/prometheus.yml', 'utf8').replace(/\r\n/g, '\n')
const uploadScript = readFileSync('upload-server.ps1', 'utf8').replace(/\r\n/g, '\n')

describe('production security defaults', () => {
  it('keeps anonymous production metrics disabled by default', () => {
    assert.match(prodCompose, /PUBLIC_METRICS_ENABLED=\$\{PUBLIC_METRICS_ENABLED:-false\}/)
    assert.doesNotMatch(prodCompose, /PUBLIC_METRICS_ENABLED=\$\{PUBLIC_METRICS_ENABLED:-true\}/)
    assert.match(envExample, /^PUBLIC_METRICS_ENABLED=false$/m)
    assert.match(prometheusConfig, /PUBLIC_METRICS_ENABLED=false/)
  })

  it('requires Redis-backed protections to fail closed in production defaults', () => {
    assert.match(prodCompose, /RATE_LIMIT_REDIS_ENABLED=\$\{RATE_LIMIT_REDIS_ENABLED:-true\}/)
    assert.match(prodCompose, /RATE_LIMIT_REDIS_FAIL_CLOSED=\$\{RATE_LIMIT_REDIS_FAIL_CLOSED:-true\}/)
    assert.match(prodCompose, /BRUTE_FORCE_REDIS_ENABLED=\$\{BRUTE_FORCE_REDIS_ENABLED:-true\}/)
    assert.match(prodCompose, /BRUTE_FORCE_REDIS_FAIL_CLOSED=\$\{BRUTE_FORCE_REDIS_FAIL_CLOSED:-true\}/)
    assert.match(envExample, /^RATE_LIMIT_REDIS_FAIL_CLOSED=true$/m)
    assert.match(envExample, /^BRUTE_FORCE_REDIS_FAIL_CLOSED=true$/m)
  })

  it('fails production compose preflight for unsafe runtime overrides', () => {
    const requiredSecrets = [
      'DB_PASSWORD',
      'MYSQL_ROOT_PASSWORD',
      'REDIS_PASSWORD',
      'GRAFANA_ADMIN_PASSWORD',
      'JWT_SECRET',
      'ADMIN_ENCRYPTION_KEY',
      'CSRF_SIGNING_SECRET',
      'CACHE_KEY_HMAC_SECRET',
      'PAYMENT_CALLBACK_SECRET',
      'PII_KEYS',
      'PII_ACTIVE_KID',
      'SUPER_ADMIN_TOTP_SECRET',
      'SCRAPLING_API_KEY',
      'RECAPTCHA_SITE_KEY',
      'RECAPTCHA_SECRET_KEY',
      'VITE_AMAP_KEY',
      'VITE_AMAP_SECURITY_CODE',
      'ALERTMANAGER_WEBHOOK_URL'
    ]
    const exactChecks = [
      ['SPRING_FLYWAY_ENABLED', 'true'],
      ['SPRING_JPA_HIBERNATE_DDL_AUTO', 'validate'],
      ['REQUIRE_STRONG_SECRETS', 'true'],
      ['COOKIE_SECURE', 'true'],
      ['PAYMENT_MOCK_CALLBACK_ENABLED', 'false'],
      ['SEED_DEMO_USERS', 'false'],
      ['SEED_CONTENT_ENABLED', 'false'],
      ['PUBLIC_METRICS_ENABLED', 'false'],
      ['RATE_LIMIT_REDIS_ENABLED', 'true'],
      ['RATE_LIMIT_REDIS_FAIL_CLOSED', 'true'],
      ['BRUTE_FORCE_REDIS_ENABLED', 'true'],
      ['BRUTE_FORCE_REDIS_FAIL_CLOSED', 'true'],
      ['ANTIBOT_ENABLED', 'true'],
      ['RECAPTCHA_ENABLED', 'true'],
      ['REGISTRATION_RECAPTCHA_REQUIRED', 'true'],
      ['DB_ALLOW_PUBLIC_KEY_RETRIEVAL', 'false'],
      ['SCRAPLING_ALLOW_UNAUTHENTICATED', 'false']
    ]

    for (const name of requiredSecrets) {
      assert.match(prodCompose, new RegExp(`${name}=\\$\\{${name}:\\?${name} is required in production\\}`))
      assert.match(prodCompose, new RegExp(`require_real_secret ${name} "\\$\\$${name}"`))
    }
    assert.match(prodCompose, /example\\.com/)
    assert.match(prodCompose, /NGINX_SERVER_NAME=\$\{NGINX_SERVER_NAME:\?NGINX_SERVER_NAME is required in production\}/)
    assert.match(prodCompose, /NGINX_REDIRECT_HOST=\$\{NGINX_REDIRECT_HOST:\?NGINX_REDIRECT_HOST is required in production\}/)
    assert.match(prodCompose, /NGINX_CERT_DOMAIN=\$\{NGINX_CERT_DOMAIN:\?NGINX_CERT_DOMAIN is required in production\}/)
    assert.match(prodCompose, /require_server_names NGINX_SERVER_NAME "\$\$NGINX_SERVER_NAME"/)
    assert.match(prodCompose, /require_single_host NGINX_REDIRECT_HOST "\$\$NGINX_REDIRECT_HOST"/)
    assert.match(prodCompose, /require_single_host NGINX_CERT_DOMAIN "\$\$NGINX_CERT_DOMAIN"/)
    assert.match(prodCompose, /SUPER_ADMIN_TOTP_SECRET must be a Base32 secret with at least 32 characters/)

    for (const [name, expected] of exactChecks) {
      assert.match(prodCompose, new RegExp(`${name}=\\$\\{${name}:-${expected}\\}`))
      assert.match(prodCompose, new RegExp(`require_exact ${name} "\\$\\$${name}" ${expected}`))
    }

    assert.match(prodCompose, /DB_SSL_MODE=\$\{DB_SSL_MODE:-REQUIRED\}/)
    assert.match(prodCompose, /DB_SSL_MODE must be REQUIRED or VERIFY_IDENTITY/)
  })

  it('validates upload-server host inputs before deriving certificate paths', () => {
    const serverNameCheck = uploadScript.indexOf('require_server_name_list_value NGINX_SERVER_NAME "$NGINX_SERVER_NAME"')
    const certDomainCheck = uploadScript.indexOf('require_single_host_value NGINX_CERT_DOMAIN "$NGINX_CERT_DOMAIN"')
    const certPathUse = uploadScript.indexOf('LE_CERT_DIR="/etc/letsencrypt/live/$NGINX_CERT_DOMAIN"')

    assert.notEqual(serverNameCheck, -1)
    assert.notEqual(certDomainCheck, -1)
    assert.notEqual(certPathUse, -1)
    assert.ok(serverNameCheck < certPathUse)
    assert.ok(certDomainCheck < certPathUse)
    assert.match(uploadScript, /must be a single DNS host without scheme/)
    assert.match(uploadScript, /entries must be DNS hosts separated by single spaces only/)
    assert.ok(uploadScript.includes('example\\.com|local-dev|local-'))
  })
})
