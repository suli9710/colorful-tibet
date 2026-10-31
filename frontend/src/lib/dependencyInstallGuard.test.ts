// @vitest-environment node
import { afterEach, describe, expect, it } from 'vitest'
import { mkdtempSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { spawnSync } from 'node:child_process'

const directories: string[] = []
afterEach(() => { directories.splice(0).forEach(path => rmSync(path, { recursive: true, force: true })) })
function isolatedBridge(omit?: string, brokenPackage = false) {
  const cwd = mkdtempSync(join(tmpdir(), 'tibet-dependency-guard-'))
  directories.push(cwd)
  const script = join(cwd, 'bridge.mjs')
  writeFileSync(script, readFileSync(new URL('../../scripts/patch-redocly-minimatch.mjs', import.meta.url)))
  if (brokenPackage) {
    const root = join(cwd, 'node_modules/openapi-typescript')
    mkdirSync(root, { recursive: true })
    writeFileSync(join(root, 'package.json'), JSON.stringify({ name: 'openapi-typescript', main: 'missing.js' }))
  }
  const env = { ...process.env }
  delete env.npm_config_omit
  delete env.NODE_PATH
  if (omit !== undefined) env.npm_config_omit = omit
  return spawnSync(process.execPath, [script], { cwd, env, encoding: 'utf8' })
}

describe('development-only install bridge omission guard', () => {
  it('copies the installation bridge before the Docker dependency layer', () => {
    const dockerfile = readFileSync(new URL('../../Dockerfile', import.meta.url), 'utf8')
    const copy = dockerfile.indexOf('COPY scripts/patch-redocly-minimatch.mjs ./scripts/patch-redocly-minimatch.mjs')
    expect(copy).toBeGreaterThan(0)
    expect(copy).toBeLessThan(dockerfile.indexOf('RUN npm ci'))
    expect(dockerfile).not.toContain('npm ci --ignore-scripts')
    const ignored = readFileSync(new URL('../../.dockerignore', import.meta.url), 'utf8').split('\n')
    expect(ignored).not.toContain('scripts/')
    expect(ignored).not.toContain('*.mjs')
  })
  it('skips only explicit dev omission when the package is absent', () => {
    const result = isolatedBridge('dev')
    expect(result.status).toBe(0)
    expect(result.stdout).toContain('explicitly omitted dev dependencies')
  })
  it('fails closed for unexplained missing dependencies', () => {
    expect(isolatedBridge().status).not.toBe(0)
    expect(isolatedBridge('optional').status).not.toBe(0)
  })
  it('does not hide an installed but broken package even during dev omission', () => {
    expect(isolatedBridge('dev', true).status).not.toBe(0)
  })
})
