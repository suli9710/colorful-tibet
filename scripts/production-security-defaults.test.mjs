import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { describe, it } from 'node:test'

const prodCompose = readFileSync('docker-compose.prod.yml', 'utf8').replace(/\r\n/g, '\n')
const envExample = readFileSync('.env.example', 'utf8').replace(/\r\n/g, '\n')
const prometheusConfig = readFileSync('monitoring/prometheus/prometheus.yml', 'utf8').replace(/\r\n/g, '\n')
const uploadScript = readFileSync('upload-server.ps1', 'utf8').replace(/\r\n/g, '\n')
const deployPreflightScript = readFileSync('scripts/deploy-preflight.ps1', 'utf8').replace(/\r\n/g, '\n')
const demoDeployScript = readFileSync('deploy-new-server-http.ps1', 'utf8').replace(/\r\n/g, '\n')
const deploymentConfiguration = readFileSync('docs/deployment-configuration.md', 'utf8').replace(/\r\n/g, '\n')
const releaseRunbook = readFileSync('docs/release-staging-runbook.md', 'utf8').replace(/\r\n/g, '\n')
const deploymentGuide = readFileSync('docs/DEPLOYMENT.md', 'utf8').replace(/\r\n/g, '\n')
const operationManualGenerator = readFileSync('tools/generate_operation_manual.py', 'utf8').replace(/\r\n/g, '\n')
const knownTotpTestVectors = [
  'GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ',
  'JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP',
  'MZXW6YTBOJQXGZJAMZXXE3DEMF2GK3LQ',
  'NBSWY3DPEB3W64TMMQXG6ZRAMZXXE3DE',
  'MFRGGZDFMZTWQ2LKMFRGGZDFMZTWQ2LK'
]

describe('production security defaults', () => {
  it('keeps anonymous production metrics disabled and authenticates the built-in scraper', () => {
    assert.match(prodCompose, /PUBLIC_METRICS_ENABLED=\$\{PUBLIC_METRICS_ENABLED:-false\}/)
    assert.doesNotMatch(prodCompose, /PUBLIC_METRICS_ENABLED=\$\{PUBLIC_METRICS_ENABLED:-true\}/)
    assert.match(envExample, /^PUBLIC_METRICS_ENABLED=false$/m)
    assert.match(prodCompose, /METRICS_SCRAPE_TOKEN=\$\{METRICS_SCRAPE_TOKEN:\?METRICS_SCRAPE_TOKEN is required in production\}/)
    assert.match(prometheusConfig, /authorization:\n\s+type: Bearer\n\s+credentials_file: \/tmp\/metrics-scrape-token/)
  })

  it('never lets Spring resolve the client IP from forwarding headers', () => {
    // "framework" registers ForwardedHeaderFilter ahead of the security chain, which overwrites
    // request.getRemoteAddr() from the leftmost X-Forwarded-For entry and strips the X-Forwarded-*
    // headers, so TrustedProxyIpResolver's trusted-CIDR walk never sees them and every IP-keyed
    // rate limit, brute-force counter and anonymous AI quota becomes forgeable.
    assert.match(prodCompose, /SERVER_FORWARD_HEADERS_STRATEGY=\$\{SERVER_FORWARD_HEADERS_STRATEGY:-none\}/)
    assert.doesNotMatch(prodCompose, /SERVER_FORWARD_HEADERS_STRATEGY=\$\{SERVER_FORWARD_HEADERS_STRATEGY:-framework\}/)
    assert.match(envExample, /^SERVER_FORWARD_HEADERS_STRATEGY=none$/m)
  })

  it('requires Redis-backed protections to fail closed in production defaults', () => {
    assert.match(prodCompose, /RATE_LIMIT_REDIS_ENABLED=\$\{RATE_LIMIT_REDIS_ENABLED:-true\}/)
    assert.match(prodCompose, /RATE_LIMIT_REDIS_FAIL_CLOSED=\$\{RATE_LIMIT_REDIS_FAIL_CLOSED:-true\}/)
    assert.match(prodCompose, /BRUTE_FORCE_REDIS_ENABLED=\$\{BRUTE_FORCE_REDIS_ENABLED:-true\}/)
    assert.match(prodCompose, /BRUTE_FORCE_REDIS_FAIL_CLOSED=\$\{BRUTE_FORCE_REDIS_FAIL_CLOSED:-true\}/)
    assert.match(prodCompose, /TOTP_REPLAY_FAIL_CLOSED=\$\{TOTP_REPLAY_FAIL_CLOSED:-true\}/)
    assert.match(envExample, /^RATE_LIMIT_REDIS_FAIL_CLOSED=true$/m)
    assert.match(envExample, /^BRUTE_FORCE_REDIS_FAIL_CLOSED=true$/m)
    assert.match(envExample, /^TOTP_REPLAY_FAIL_CLOSED=true$/m)
    assert.match(uploadScript, /require_exact_env TOTP_REPLAY_FAIL_CLOSED true/)
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
      'METRICS_SCRAPE_TOKEN',
      'PAYMENT_CALLBACK_SECRET',
      'PII_KEYS',
      'PII_ACTIVE_KID',
      'SUPER_ADMIN_TOTP_SECRET',
      'SCRAPLING_API_KEY',
      'RECAPTCHA_SITE_KEY',
      'RECAPTCHA_SECRET_KEY',
      'VITE_AMAP_KEY',
      'VITE_AMAP_SECURITY_CODE',
      'ALERTMANAGER_WEBHOOK_URL',
      'APP_VERSION'
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
      ['TOTP_REPLAY_FAIL_CLOSED', 'true'],
      ['ANTIBOT_ENABLED', 'true'],
      ['ANTIBOT_LOG_RETENTION_ENABLED', 'true'],
      ['RECAPTCHA_ENABLED', 'true'],
      ['REGISTRATION_RECAPTCHA_REQUIRED', 'true'],
      ['DB_ALLOW_PUBLIC_KEY_RETRIEVAL', 'false'],
      ['SCRAPLING_ALLOW_UNAUTHENTICATED', 'false'],
      ['SERVER_FORWARD_HEADERS_STRATEGY', 'none'],
      ['TRUST_PROXY_HEADERS', 'true']
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
    assert.match(prodCompose, /SUPER_ADMIN_TOTP_SECRET must be a high-entropy Base32 secret in complete 8-character blocks/)
    assert.match(
      prodCompose,
      /require_int_range ANTIBOT_LOG_RETENTION_DAYS "\$\$ANTIBOT_LOG_RETENTION_DAYS" 1 365/
    )
    assert.match(prodCompose, /must be an integer between \$\$minimum and \$\$maximum/)

    for (const [name, expected] of exactChecks) {
      assert.match(prodCompose, new RegExp(`${name}=\\$\\{${name}:-${expected}\\}`))
      assert.match(prodCompose, new RegExp(`require_exact ${name} "\\$\\$${name}" ${expected}`))
    }

    assert.match(prodCompose, /DB_SSL_MODE=\$\{DB_SSL_MODE:-REQUIRED\}/)
    assert.match(prodCompose, /DB_SSL_MODE must be REQUIRED or VERIFY_IDENTITY/)
    assert.match(prodCompose, /FRONTEND_CONTAINER_IP=\$\{FRONTEND_CONTAINER_IP:-172\.28\.0\.10\}/)
    assert.match(
      prodCompose,
      /require_exact TRUSTED_PROXY_CIDRS "\$\$TRUSTED_PROXY_CIDRS" "\$\$FRONTEND_CONTAINER_IP\/32"/
    )
  })

  it('runs Redis without root privileges and bounds Docker log growth', () => {
    const redisBlock = prodCompose.match(/\n  redis:\n([\s\S]*?)\n  mysql:\n/)?.[1] || ''
    assert.match(redisBlock, /\n    user: redis\n/)
    assert.match(redisBlock, /\n    read_only: true\n/)
    assert.match(redisBlock, /\n    cap_drop:\n      - ALL\n/)
    assert.match(prodCompose, /x-default-logging: &default-logging/)
    assert.match(prodCompose, /driver: local/)
    assert.match(prodCompose, /max-size: "10m"/)
    assert.match(prodCompose, /max-file: "5"/)
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

  it('passes and validates independent administrator TOTP configuration', () => {
    assert.match(prodCompose, /ADMIN_TOTP_SECRETS=\$\{ADMIN_TOTP_SECRETS:-\}/)
    assert.match(prodCompose, /validate_admin_totp_secrets "\$\$ADMIN_TOTP_SECRETS"/)
    assert.match(prodCompose, /ADMIN_TOTP_SECRETS secrets must be independent/)
    assert.match(prodCompose, /is_strong_totp_secret/)
    assert.match(prodCompose, /is_sequential_base32_pattern/)
    assert.match(prodCompose, /is_periodic_totp_pattern/)
    assert.match(prodCompose, /\$\$\(\( \$\$\{#secret\} % 8 \)\)/)
    assert.ok(prodCompose.includes('if ! is_strong_totp_secret "$$secret"; then'))
    assert.ok(prodCompose.includes("super_username=$(printf '%s' \"$$super_username\" | sed 's/^[[:space:]]*//; s/[[:space:]]*$$//'"))
    assert.match(uploadScript, /validate_admin_totp_secrets_value/)
    assert.match(uploadScript, /is_strong_totp_secret/)
    assert.match(uploadScript, /is_sequential_base32_pattern/)
    assert.match(uploadScript, /is_periodic_totp_pattern/)
    assert.match(uploadScript, /\$\(\( \$\{#secret\} % 8 \)\)/)
    assert.ok(uploadScript.includes('if ! is_strong_totp_secret "$secret"; then'))
    assert.match(uploadScript, /ADMIN_TOTP_SECRETS_VALUE=\$\(get_env_value ADMIN_TOTP_SECRETS\)/)
    assert.ok(uploadScript.includes("super_username=$(printf '%s' \"$super_username\" | sed 's/^[[:space:]]*//; s/[[:space:]]*$//'"))
    assert.match(deployPreflightScript, /Test-SequentialBase32Pattern/)
    assert.match(deployPreflightScript, /Test-PeriodicTotpPattern/)
    assert.match(deployPreflightScript, /-cnotmatch '\^\[A-Z2-7\]\{32,\}\$'/)
    for (const knownVector of knownTotpTestVectors) {
      assert.ok(prodCompose.includes(knownVector))
      assert.ok(uploadScript.includes(knownVector))
      assert.ok(deployPreflightScript.includes(knownVector))
    }
    assert.match(demoDeployScript, /"adminTotpSecret": b32\(20\)/)
    assert.match(demoDeployScript, /SEED_DEMO_USERS=true/)
    assert.match(demoDeployScript, /ADMIN_TOTP_SECRETS=admin=\$\(\$Secrets\.adminTotpSecret\)/)
    assert.match(demoDeployScript, /TOTP_REPLAY_FAIL_CLOSED=false/)
    assert.match(demoDeployScript, /Admin TOTP URI .*otpauth:\/\/totp\/ColorfulTibet:admin\?secret=/)
  })

  it('documents coordinated cache-HMAC rotation because it binds replay scopes and admin JWTs', () => {
    for (const document of [deploymentConfiguration, releaseRunbook]) {
      assert.match(document, /CACHE_KEY_HMAC_SECRET/)
      assert.match(document, /replay-scope/)
      assert.match(document, /mfb/)
      assert.match(document, /stop or drain/)
      assert.match(document, /fully restart every instance/)
      assert.match(document, /Never use a\s+rolling mixed-key deployment/)
    }
  })

  it('does not publish a copyable TOTP credential in deployment documentation or its generator', () => {
    for (const documentSource of [deploymentGuide, operationManualGenerator]) {
      for (const knownVector of knownTotpTestVectors) {
        assert.doesNotMatch(documentSource, new RegExp(knownVector))
      }
      assert.match(documentSource, /CSPRNG-generated-160-bit-Base32-secret/)
    }
  })
})
