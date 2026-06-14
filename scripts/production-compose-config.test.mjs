import assert from 'node:assert/strict'
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { afterEach, describe, it } from 'node:test'

const tempRoots = []

const productionLikeEnv = () => [
  readFileSync('.env.example', 'utf8').replace(/\r\n/g, '\n'),
  'NGINX_SERVER_NAME=colorfultibet.test www.colorfultibet.test',
  'NGINX_REDIRECT_HOST=colorfultibet.test',
  'NGINX_CERT_DOMAIN=colorfultibet.test',
  'ALERTMANAGER_WEBHOOK_URL=https://alerts.colorfultibet.test/webhook'
].join('\n')

const runComposeConfig = (envSource, composeFile = 'docker-compose.prod.yml') => {
  const root = mkdtempSync(path.join(tmpdir(), 'ct-compose-config-'))
  tempRoots.push(root)
  const envPath = path.join(root, '.env')
  writeFileSync(envPath, envSource)

  return spawnSync('docker', [
    'compose',
    '--env-file',
    envPath,
    '-f',
    composeFile,
    'config',
    '--quiet'
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

describe('production compose config fixture', () => {
  it('parses with production-like non-placeholder edge hosts', function () {
    const result = runComposeConfig(productionLikeEnv())
    if (result.error && result.error.code === 'ENOENT') {
      this.skip('Docker CLI is not available in this environment')
      return
    }

    assert.equal(result.status, 0, result.stderr || result.stdout)
  })
})
