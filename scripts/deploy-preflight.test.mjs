import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { spawnSync } from 'node:child_process'
import { describe, it } from 'node:test'

const scriptPath = 'scripts/deploy-preflight.ps1'
const source = readFileSync(scriptPath, 'utf8')

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
})
