// Compatibility bridge until openapi-typescript supports Redocly 2's public API.
// Redocly 1 uses minimatch's pre-v9 callable CommonJS export. We install the
// patched minimatch 11 implementation, adapting only this import (not its logic).
import { createHash } from 'node:crypto'
import { createRequire } from 'node:module'
import { dirname, join } from 'node:path'
import { lstatSync, readFileSync, writeFileSync } from 'node:fs'
const require = createRequire(import.meta.url)
let openapiEntry
try {
  openapiEntry = require.resolve('openapi-typescript')
} catch (error) {
  const intentionallyOmitted = (process.env.npm_config_omit || '').split(/[\s,]+/).includes('dev')
  const packagePresent = (require.resolve.paths('openapi-typescript') || [])
    .some(path => {
      try { lstatSync(join(path, 'openapi-typescript')); return true }
      catch (inspectionError) {
        if (inspectionError.code === 'ENOENT') return false
        throw inspectionError
      }
    })
  if (intentionallyOmitted && !packagePresent && error.code === 'MODULE_NOT_FOUND') {
    console.log('Redocly development-only bridge skipped: npm explicitly omitted dev dependencies')
    process.exit(0)
  }
  throw error
}
const openapiRequire = createRequire(openapiEntry)
const redoclyEntry = openapiRequire.resolve('@redocly/openapi-core')
const redoclyRequire = createRequire(redoclyEntry)
if (redoclyRequire('minimatch/package.json').version !== '11.0.1' || typeof redoclyRequire('minimatch').minimatch !== 'function') {
  throw new Error('Redocly requires patched minimatch 11.0.1; refresh the lockfile override')
}
const root = dirname(dirname(redoclyEntry))
const pkg = JSON.parse(readFileSync(join(root, 'package.json'), 'utf8'))
if (pkg.version !== '1.34.22') throw new Error(`Review Redocly minimatch bridge for unexpected version ${pkg.version}`)
const filename = join(root, 'lib/utils.js')
const original = 'const minimatch = require("minimatch");'
const replacement = 'const { minimatch } = require("minimatch");'
const contents = readFileSync(filename, 'utf8')
const hash = createHash('sha256').update(contents).digest('hex')
const originalHash = '896a9acce92e3e5905aa267e895985d57888a87f500d58c29da9c8f9d488ff1c'
const patchedHash = '825736f13fb67dc4a1e2157bb51396209c7343cfd00376ed5d923e3e9ff82a18'
if (hash !== originalHash && hash !== patchedHash) throw new Error('Redocly source hash changed; review compatibility bridge')
if (hash === originalHash) {
  if (contents.split(original).length !== 2) throw new Error('Unexpected Redocly minimatch import count')
  writeFileSync(filename, contents.replace(original, replacement))
} else if (!contents.includes(replacement)) {
  throw new Error('Redocly minimatch bridge no longer matches; review before installing')
}
console.log('Redocly 1.34.22: using patched minimatch 11 named API')
