import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { describe, it } from 'node:test'

const prodCompose = readFileSync('docker-compose.prod.yml', 'utf8').replace(/\r\n/g, '\n')
const envExample = readFileSync('.env.example', 'utf8').replace(/\r\n/g, '\n')
const prometheusConfig = readFileSync('monitoring/prometheus/prometheus.yml', 'utf8').replace(/\r\n/g, '\n')

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
    const exactChecks = [
      ['REQUIRE_STRONG_SECRETS', 'true'],
      ['COOKIE_SECURE', 'true'],
      ['PAYMENT_MOCK_CALLBACK_ENABLED', 'false'],
      ['PUBLIC_METRICS_ENABLED', 'false'],
      ['RATE_LIMIT_REDIS_ENABLED', 'true'],
      ['RATE_LIMIT_REDIS_FAIL_CLOSED', 'true'],
      ['BRUTE_FORCE_REDIS_ENABLED', 'true'],
      ['BRUTE_FORCE_REDIS_FAIL_CLOSED', 'true'],
      ['DB_ALLOW_PUBLIC_KEY_RETRIEVAL', 'false'],
      ['SCRAPLING_ALLOW_UNAUTHENTICATED', 'false']
    ]

    for (const [name, expected] of exactChecks) {
      assert.match(prodCompose, new RegExp(`${name}=\\$\\{${name}:-${expected}\\}`))
      assert.match(prodCompose, new RegExp(`require_exact ${name} "\\$\\$${name}" ${expected}`))
    }

    assert.match(prodCompose, /DB_SSL_MODE=\$\{DB_SSL_MODE:-REQUIRED\}/)
    assert.match(prodCompose, /DB_SSL_MODE must be REQUIRED or VERIFY_IDENTITY/)
  })
})
