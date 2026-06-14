/// <reference types="node" />

import { readFileSync } from 'node:fs'
import * as path from 'node:path'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

const repoRoot = fileURLToPath(new URL('../../../', import.meta.url))

const readRepoFile = (...segments: string[]) =>
  readFileSync(path.join(repoRoot, ...segments), 'utf8')

const envValue = (source: string, name: string) => {
  const match = source.match(new RegExp(`^${name}=(.*)$`, 'm'))
  return match?.[1]?.trim()
}

const serviceBlock = (composeSource: string, serviceName: string) => {
  const normalized = composeSource.replace(/\r\n/g, '\n')
  const pattern = new RegExp(`\\n  ${serviceName}:\\n[\\s\\S]*?(?=\\n  [a-zA-Z0-9_-]+:\\n|\\nnetworks:|\\nvolumes:|$)`)
  const match = normalized.match(pattern)
  if (!match) {
    throw new Error(`Missing compose service: ${serviceName}`)
  }
  return match[0]
}

const normalizedRequirementLines = (source: string) =>
  source
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line && !line.startsWith('#'))

const packageRequirementLines = (source: string) =>
  normalizedRequirementLines(source)
    .filter((line) => /^[A-Za-z0-9_.-]+(?:\[[^\]]+\])?==[A-Za-z0-9_.!+/-]+(?:\s+\\)?$/.test(line))
    .map((line) => line.replace(/\s+\\$/, ''))

describe('ops configuration guardrails', () => {
  it('keeps infrastructure password examples off known placeholder passwords', () => {
    const envExample = readRepoFile('.env.example')
    const infrastructurePasswordVars = [
      'MYSQL_PASSWORD',
      'MYSQL_ROOT_PASSWORD',
      'DB_PASSWORD',
      'REDIS_PASSWORD',
      'GRAFANA_ADMIN_PASSWORD'
    ]

    for (const variableName of infrastructurePasswordVars) {
      const value = envValue(envExample, variableName)

      expect(value, variableName).toBeTruthy()
      expect(value, variableName).not.toMatch(/change-me|changeme/i)
    }
  })

  it('keeps production proxy trust disabled unless operators opt into explicit CIDRs', () => {
    const envExample = readRepoFile('.env.example')
    const prodCompose = readRepoFile('docker-compose.prod.yml')

    expect(envValue(envExample, 'TRUST_PROXY_HEADERS')).toBe('false')
    expect(envValue(envExample, 'TRUSTED_PROXY_CIDRS')).toBe('')
    expect(prodCompose).toContain('TRUST_PROXY_HEADERS=${TRUST_PROXY_HEADERS:-false}')
    expect(prodCompose).toContain('TRUSTED_PROXY_CIDRS=${TRUSTED_PROXY_CIDRS:-}')
    expect(prodCompose).not.toMatch(/TRUSTED_PROXY_CIDRS=\$\{TRUSTED_PROXY_CIDRS:-[^}\n]*(?:10\.0\.0\.0\/8|172\.16\.0\.0\/12|192\.168\.0\.0\/16)/)
    expect(prodCompose).toContain('TRUSTED_PROXY_CIDRS must not trust broad private ranges')
  })

  it('runs production preflight before services that consume infrastructure secrets', () => {
    const prodCompose = readRepoFile('docker-compose.prod.yml')
    const uploadServer = readRepoFile('upload-server.ps1')
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
    ] as const

    expect(prodCompose).toContain('production-preflight:')
    for (const variableName of requiredSecrets) {
      expect(prodCompose, variableName).toContain(`require_real_secret ${variableName}`)
      expect(uploadServer, variableName).toContain(variableName)
    }
    expect(prodCompose).toContain('CACHE_KEY_HMAC_SECRET must be at least 64 characters')
    expect(prodCompose).toContain('reject_placeholder_if_set MYSQL_PASSWORD')
    expect(prodCompose).toContain('require_single_host NGINX_REDIRECT_HOST "$$NGINX_REDIRECT_HOST"')
    expect(prodCompose).toContain('must be a single DNS host without scheme, path, port, comma, whitespace, or control characters')
    expect(uploadServer).toContain('require_single_host_value NGINX_REDIRECT_HOST "$NGINX_REDIRECT_HOST"')
    expect(uploadServer).toContain('must be a single DNS host without scheme, path, port, comma, whitespace, or control characters')
    expect(uploadServer).toContain('for secret_name in \\')
    expect(uploadServer).toContain('CACHE_KEY_HMAC_SECRET must be at least 64 characters')
    expect(uploadServer).toContain('TRUSTED_PROXY_CIDRS must not trust broad private ranges')
  })

  it('hardens the production scrapling service while keeping writable temp space', () => {
    const prodCompose = readRepoFile('docker-compose.prod.yml')
    const scrapling = serviceBlock(prodCompose, 'scrapling')
    const dockerfile = readRepoFile('scrapler', 'Dockerfile')

    expect(scrapling).toContain('security_opt:')
    expect(scrapling).toContain('no-new-privileges:true')
    expect(scrapling).toContain('cap_drop:')
    expect(scrapling).toContain('- ALL')
    expect(scrapling).toContain('read_only: true')
    expect(scrapling).toContain('/tmp:rw,noexec,nosuid,nodev')
    expect(dockerfile).toContain('PYTHONDONTWRITEBYTECODE=1')
    expect(dockerfile).toContain('HOME=/tmp')
  })

  it('keeps the remote frontend helper free of hard-coded plaintext public backends', () => {
    const startRemote = readRepoFile('frontend', 'start-remote.bat')

    expect(startRemote).toContain('REMOTE_BACKEND is required')
    expect(startRemote).toContain('Plain HTTP remote backends are refused')
    expect(startRemote).not.toMatch(/set\s+"REMOTE_BACKEND=http:\/\/(?!localhost|127\.0\.0\.1|\[::1\])/i)
  })

  it('keeps nginx multi-domain server names separate from the canonical redirect host', () => {
    const envExample = readRepoFile('.env.example')
    const nginxConf = readRepoFile('frontend', 'nginx.conf')
    const frontendDockerfile = readRepoFile('frontend', 'Dockerfile')
    const prodCompose = readRepoFile('docker-compose.prod.yml')
    const redirectHost = envValue(envExample, 'NGINX_REDIRECT_HOST')

    expect(envValue(envExample, 'NGINX_SERVER_NAME')).toContain(' ')
    expect(redirectHost).toBeTruthy()
    expect(redirectHost).not.toMatch(/\s/)
    expect(nginxConf).toContain('server_name ${NGINX_SERVER_NAME};')
    expect(nginxConf).toContain('return 301 https://${NGINX_REDIRECT_HOST}$request_uri;')
    expect(nginxConf).not.toContain('return 301 https://${NGINX_SERVER_NAME}$request_uri;')
    expect(frontendDockerfile).toContain('ENV NGINX_REDIRECT_HOST=localhost')
    expect(frontendDockerfile).toContain('$NGINX_REDIRECT_HOST')
    expect(prodCompose).toContain('NGINX_REDIRECT_HOST=${NGINX_REDIRECT_HOST:?NGINX_REDIRECT_HOST is required in production}')
    expect(prodCompose).toContain('is_dns_host()')
    expect(prodCompose).toContain('require_server_names NGINX_SERVER_NAME "$$NGINX_SERVER_NAME"')
    expect(prodCompose).toContain('require_single_host NGINX_REDIRECT_HOST "$$NGINX_REDIRECT_HOST"')
    expect(prodCompose).toContain('must be a single DNS host without scheme, path, port, comma, whitespace, or control characters')
  })

  it('keeps scrapler runtime requirements exact-pinned in the hash-locked requirements file', () => {
    const requirements = readRepoFile('scrapler', 'requirements.txt')
    const lines = packageRequirementLines(requirements)

    expect(lines).toEqual(expect.arrayContaining([
      'fastapi==0.136.1',
      'uvicorn[standard]==0.47.0',
      'httpx==0.28.1',
      'scrapling[fetchers]==0.4.8',
      'lxml==6.1.1',
      'python-dotenv==1.2.2',
      'pydantic==2.13.4'
    ]))

    for (const line of lines) {
      expect(line, line).toMatch(/^[A-Za-z0-9_.-]+(?:\[[^\]]+\])?==[A-Za-z0-9_.!+/-]+$/)
      expect(line, line).not.toMatch(/[<>~]/)
    }

    expect(requirements).toContain('--hash=sha256:')
  })

  it('keeps CI fail-closed until external supply-chain pins are filled in', () => {
    const ci = readRepoFile('.github', 'workflows', 'ci.yml')
    const supplyChainGate = readRepoFile('scripts', 'check-supply-chain-pins.mjs')
    const packageJson = readRepoFile('package.json')

    expect(ci).toContain('supply-chain-pin-gate:')
    expect(ci).toContain('Supply chain pin gate')
    expect(ci).toContain('npm --prefix frontend ci')
    expect(ci).toContain('node scripts/validate-workflow-yaml.mjs')
    expect(ci).not.toContain('go install github.com/rhysd/actionlint')
    expect(ci).not.toContain('python -m pip install --upgrade pip')
    expect(ci).not.toContain('python -m pip install yamllint')
    expect(packageJson).toContain('--test-concurrency=1')
    expect(packageJson).toContain('scripts/check-supply-chain-pins.test.mjs scripts/resolve-docker-image-digests.test.mjs scripts/validate-workflow-yaml.test.mjs')
    expect(packageJson).toContain('scripts/deploy-preflight.test.mjs')
    expect(ci).toContain('node scripts/check-supply-chain-pins.mjs')
    expect(supplyChainGate).toContain('GitHub Actions must use full commit SHAs before production release.')
    expect(supplyChainGate).toContain('Dockerfile base images must include sha256 digests before production release.')
    expect(supplyChainGate).toContain('Compose service images must include sha256 digests before production release.')
    expect(supplyChainGate).toContain('Workflow container image references must include sha256 digests before production release.')
    expect(supplyChainGate).toContain('Workflow tool installs must use repository lockfiles or checked-in tools before production release.')
    expect(supplyChainGate).toContain('Scrapler exact pins must be backed by pip --hash=sha256 entries before production release.')
    expect(supplyChainGate).toContain('Scrapler installs must use pip --require-hashes before production release.')
    expect(supplyChainGate).toContain('intentionally fail-closed')
  })

  it('documents release supply-chain pin status and remaining external image digests', () => {
    const docs = readRepoFile('docs', 'deployment-configuration.md')
    const checklist = readRepoFile('docs', 'release-supply-chain-pin-checklist.md')
    const resolver = readRepoFile('scripts', 'resolve-docker-image-digests.mjs')
    const requiredFragments = [
      'Supply-chain Pinning',
      'node scripts/resolve-docker-image-digests.mjs --markdown',
      'scripts/validate-workflow-yaml.mjs',
      'workflow tool install',
      '34e114876b0b11c390a56381ad16ebd13914f8d5',
      'a26af69be951a213d495a4c3e4e4022e16d87065',
      'dcedce43c6f43de0b836d1fe38946645c9c638dc',
      'c1e323688fd81a25caa38c78aa6df2d33d3e20d9',
      'a9c7b0f06e461e9d4b4d1711f154ee024b8d7ab8',
      '49933ea5288caeca8642d1e84afbd3f7d6820020',
      '8d2750c68a42422c14e847fe6c8ac0403b4cbd6f',
      'ea165f8d65b6e75b540449e92b4886f43607fa02',
      'maven:3.9-eclipse-temurin-17',
      'eclipse-temurin:17-jre-alpine',
      'node:22-alpine',
      'nginx:alpine',
      'python:3.11-slim',
      'alpine:3.21',
      'redis:7-alpine',
      'mysql:8.4',
      'prom/prometheus:v2.55.1',
      'prom/alertmanager:v0.27.0',
      'grafana/grafana:11.4.0',
      'openzipkin/zipkin:3.4',
      'pip --require-hashes',
      'pytest==8.4.2',
      'npm run check:supply-chain-pins',
      'npm run test:ops',
      'docs/release-supply-chain-pin-checklist.md'
    ]

    for (const fragment of requiredFragments) {
      expect(docs, fragment).toContain(fragment)
    }
    expect(checklist).toContain('node scripts/resolve-docker-image-digests.mjs --markdown')
    expect(resolver).toContain('https://auth.docker.io/token')
    expect(resolver).toContain('https://registry-1.docker.io/v2/')
  })
})
