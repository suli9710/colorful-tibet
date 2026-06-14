import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { REQUIRED_IMAGE_REFS } from './resolve-docker-image-digests.mjs'

const DEFAULT_PATHS = {
  workflows: ['.github/workflows/ci.yml'],
  dockerfiles: ['backend/Dockerfile', 'frontend/Dockerfile', 'scrapler/Dockerfile'],
  composeFiles: ['docker-compose.yml', 'docker-compose.prod.yml'],
  requirementsFiles: ['scrapler/requirements.txt', 'scrapler/requirements-dev.txt'],
  pipInstallFiles: ['.github/workflows/ci.yml', 'scrapler/Dockerfile']
}
const DEFAULT_DOCKER_DIGEST_EVIDENCE_PATH = 'docker-digest-evidence.json'

const FINDING_TITLES = {
  actionSha: 'GitHub Actions must use full commit SHAs before production release.',
  dockerfileDigest: 'Dockerfile base images must include sha256 digests before production release.',
  composeDigest: 'Compose service images must include sha256 digests before production release.',
  workflowImageDigest: 'Workflow container image references must include sha256 digests before production release.',
  workflowToolInstall: 'Workflow tool installs must use repository lockfiles or checked-in tools before production release.',
  pythonExactPin: 'Scrapler requirements must use exact == pins before production release.',
  pythonHash: 'Scrapler exact pins must be backed by pip --hash=sha256 entries before production release.',
  pipRequireHashes: 'Scrapler installs must use pip --require-hashes before production release.',
  dockerDigestEvidence: 'Docker digest evidence must be complete, source-matched, and sha256-formatted.'
}

const FULL_ACTION_SHA_PATTERN = /^[0-9a-fA-F]{40}$/
const IMAGE_DIGEST_PATTERN = /@sha256:[0-9a-f]{64}(?=$|[\s\\#])/i
const PYTHON_REQUIREMENT_PATTERN = /^[A-Za-z0-9_.-]+(?:\[[^\]]+\])?\s*(?:===|==|~=|!=|<=|>=|<|>)/
const PYTHON_EXACT_PIN_PATTERN = /^[A-Za-z0-9_.-]+(?:\[[^\]]+\])?==[A-Za-z0-9_.!+*-]+(?:\s*;.+)?(?:\s|\\|$)/
const PYTHON_HASH_PATTERN = /--hash=sha256:[0-9a-f]{64}\b/i

const scriptPath = fileURLToPath(import.meta.url)

const normalizePath = (value) => value.replace(/\\/g, '/')

const readRepoFile = (repoRoot, relativePath) => {
  const fullPath = path.join(repoRoot, relativePath)
  if (!existsSync(fullPath)) {
    return null
  }
  return readFileSync(fullPath, 'utf8')
}

const lineRecords = (source) =>
  source.split(/\r?\n/).map((content, index) => ({
    line: index + 1,
    content
  }))

const addFinding = (findings, kind, file, line, content) => {
  findings.push({
    kind,
    title: FINDING_TITLES[kind],
    file,
    line,
    content: content.trim()
  })
}

const addEvidenceFinding = (findings, content) => {
  addFinding(findings, 'dockerDigestEvidence', 'docker-digest-evidence', 0, content)
}

const hasImageDigest = (imageRef) => IMAGE_DIGEST_PATTERN.test(imageRef)

const stripRequirementComment = (line) => line.replace(/\s+#.*$/, '').trimEnd()

const checkWorkflowActions = (repoRoot, workflows, findings) => {
  for (const file of workflows) {
    const source = readRepoFile(repoRoot, file)
    if (source === null) continue

    for (const record of lineRecords(source)) {
      const match = record.content.match(/^\s*(?:-\s*)?uses:\s*([^#\s]+)(?:\s|#|$)/)
      if (!match) continue

      const usesValue = match[1]
      if (usesValue.startsWith('./') || usesValue.startsWith('../')) continue

      if (usesValue.startsWith('docker://')) {
        if (!hasImageDigest(usesValue)) {
          addFinding(findings, 'workflowImageDigest', file, record.line, record.content)
        }
        continue
      }

      const atIndex = usesValue.lastIndexOf('@')
      const revision = atIndex >= 0 ? usesValue.slice(atIndex + 1) : ''
      if (!FULL_ACTION_SHA_PATTERN.test(revision)) {
        addFinding(findings, 'actionSha', file, record.line, record.content)
      }
    }
  }
}

const dockerfileBaseImage = (line) => {
  const match = line.match(/^\s*FROM\s+(.+?)(?:\s+#.*)?$/i)
  if (!match) return null

  let remainder = match[1].trim()
  while (remainder.startsWith('--')) {
    const nextSpace = remainder.search(/\s/)
    if (nextSpace < 0) return null
    remainder = remainder.slice(nextSpace).trim()
  }

  return remainder.split(/\s+/)[0]
}

const checkDockerfileBases = (repoRoot, dockerfiles, findings) => {
  for (const file of dockerfiles) {
    const source = readRepoFile(repoRoot, file)
    if (source === null) continue

    for (const record of lineRecords(source)) {
      const imageRef = dockerfileBaseImage(record.content)
      if (!imageRef || imageRef === 'scratch') continue
      if (!hasImageDigest(imageRef)) {
        addFinding(findings, 'dockerfileDigest', file, record.line, record.content)
      }
    }
  }
}

const checkComposeImages = (repoRoot, composeFiles, findings) => {
  for (const file of composeFiles) {
    const source = readRepoFile(repoRoot, file)
    if (source === null) continue

    for (const record of lineRecords(source)) {
      const match = record.content.match(/^\s*image:\s*([^#\s]+)/)
      if (!match) continue

      if (!hasImageDigest(match[1])) {
        addFinding(findings, 'composeDigest', file, record.line, record.content)
      }
    }
  }
}

const looksLikeExternalWorkflowImage = (value) => {
  const imageRef = value.replace(/\\$/, '').trim().split(/\s+/)[0]
  if (!imageRef || imageRef.startsWith('-') || imageRef.includes('$')) {
    return null
  }
  if (!/^[a-z0-9][a-z0-9._-]*(?:\/[a-z0-9][a-z0-9._-]*)+:[^\s\\]+/i.test(imageRef)) {
    return null
  }
  return imageRef
}

const workflowDockerImages = (trimmedLine) => {
  const line = trimmedLine.replace(/\\$/, '').trim()
  const images = []
  const directImage = looksLikeExternalWorkflowImage(line)
  if (directImage) {
    images.push(directImage)
  }

  if (/\bdocker\s+(?:container\s+)?run\b/i.test(line)) {
    const tokens = line.split(/\s+/)
    for (let index = 0; index < tokens.length; index += 1) {
      const token = tokens[index]
      if (!token || token === 'docker' || token === 'container' || token === 'run') continue
      if (token.startsWith('-')) {
        if (['-e', '--env', '-v', '--volume', '-w', '--workdir', '--name', '--network', '--entrypoint', '-u', '--user']
          .includes(token)) {
          index += 1
        } else if (/^--(?:env|volume|workdir|name|network|entrypoint|user)=/.test(token)) {
          continue
        }
        continue
      }

      const runImage = looksLikeExternalWorkflowImage(token)
      if (runImage) {
        images.push(runImage)
      }
      break
    }
  }

  return [...new Set(images)]
}

const checkWorkflowDockerImages = (repoRoot, workflows, findings) => {
  for (const file of workflows) {
    const source = readRepoFile(repoRoot, file)
    if (source === null) continue

    for (const record of lineRecords(source)) {
      for (const imageRef of workflowDockerImages(record.content.trim())) {
        if (!hasImageDigest(imageRef)) {
          addFinding(findings, 'workflowImageDigest', file, record.line, record.content)
        }
      }
    }
  }
}

const checkWorkflowToolInstalls = (repoRoot, workflows, findings) => {
  for (const file of workflows) {
    const source = readRepoFile(repoRoot, file)
    if (source === null) continue

    for (const record of lineRecords(source)) {
      const trimmed = record.content.trim()
      if (!trimmed || trimmed.startsWith('#')) continue

      if (/\bgo\s+install\b/.test(trimmed)) {
        addFinding(findings, 'workflowToolInstall', file, record.line, record.content)
        continue
      }

      if (/\bnpm\s+install(?:\s|$)/.test(trimmed)) {
        addFinding(findings, 'workflowToolInstall', file, record.line, record.content)
        continue
      }

      if (/\bpip\s+install\b/.test(trimmed)) {
        const installsHashLockedRequirements =
          /(?:\s-r\s+|--requirement(?:=|\s+))(?:\.\/)?(?:scrapler\/)?requirements(?:-dev)?\.txt\b/.test(trimmed) &&
          /\s--require-hashes(?:\s|$)/.test(trimmed)
        if (!installsHashLockedRequirements) {
          addFinding(findings, 'workflowToolInstall', file, record.line, record.content)
        }
      }
    }
  }
}

const parseRequirementEntries = (source) => {
  const entries = []
  let current = null

  const flush = () => {
    if (current) {
      entries.push(current)
      current = null
    }
  }

  for (const record of lineRecords(source)) {
    const cleaned = stripRequirementComment(record.content)
    const trimmed = cleaned.trim()
    if (!trimmed || trimmed.startsWith('#')) continue

    const isInclude = /^(-r|--requirement)(?:\s+|=)\S+/.test(trimmed)
    const startsRequirement = PYTHON_REQUIREMENT_PATTERN.test(trimmed)
    const continuesCurrent =
      current &&
      (/^--hash=sha256:/i.test(trimmed) ||
        /^\s+/.test(cleaned) ||
        current.hasContinuation)

    if (isInclude) {
      flush()
      entries.push({
        kind: 'include',
        line: record.line,
        lines: [trimmed]
      })
      continue
    }

    if (startsRequirement) {
      flush()
      current = {
        kind: 'requirement',
        line: record.line,
        lines: [trimmed],
        hasContinuation: trimmed.endsWith('\\')
      }
      continue
    }

    if (continuesCurrent) {
      current.lines.push(trimmed)
      current.hasContinuation = trimmed.endsWith('\\')
      continue
    }

    flush()
    entries.push({
      kind: 'unsupported',
      line: record.line,
      lines: [trimmed]
    })
  }

  flush()
  return entries
}

const checkRequirementPins = (repoRoot, requirementsFiles, findings) => {
  for (const file of requirementsFiles) {
    const source = readRepoFile(repoRoot, file)
    if (source === null) continue

    for (const entry of parseRequirementEntries(source)) {
      if (entry.kind === 'include') continue

      const firstLine = entry.lines[0].replace(/\\$/, '').trim()
      const combined = entry.lines.join(' ')
      if (entry.kind !== 'requirement' || !PYTHON_EXACT_PIN_PATTERN.test(firstLine)) {
        addFinding(findings, 'pythonExactPin', file, entry.line, entry.lines[0])
        continue
      }

      if (!PYTHON_HASH_PATTERN.test(combined)) {
        addFinding(findings, 'pythonHash', file, entry.line, entry.lines[0])
      }
    }
  }
}

const pipInstallCommands = (source) => {
  const commands = []
  let current = null

  const flush = () => {
    if (current) {
      commands.push(current)
      current = null
    }
  }

  for (const record of lineRecords(source)) {
    const line = record.content
    const trimmed = line.trim()

    if (current) {
      current.lines.push(trimmed)
      current.hasContinuation = /\\$/.test(trimmed)
      if (!current.hasContinuation) {
        flush()
      }
      continue
    }

    if (/\bpip\s+install\b/.test(trimmed)) {
      current = {
        line: record.line,
        lines: [trimmed],
        hasContinuation: /\\$/.test(trimmed)
      }
      if (!current.hasContinuation) {
        flush()
      }
    }
  }

  flush()
  return commands
}

const checkPipInstallRequireHashes = (repoRoot, files, findings) => {
  for (const file of files) {
    const source = readRepoFile(repoRoot, file)
    if (source === null) continue

    for (const command of pipInstallCommands(source)) {
      const combined = command.lines.join(' ')
      const installsRequirements = /(?:\s-r\s+|--requirement(?:=|\s+))(?:\.\/)?(?:scrapler\/)?requirements(?:-dev)?\.txt\b/.test(combined)
      if (installsRequirements && !/\s--require-hashes(?:\s|$)/.test(combined)) {
        addFinding(findings, 'pipRequireHashes', file, command.line, command.lines[0])
      }
    }
  }
}

export const collectSupplyChainPinFindings = (repoRoot, paths = DEFAULT_PATHS) => {
  const findings = []
  const normalizedRoot = path.resolve(repoRoot)

  checkWorkflowActions(normalizedRoot, paths.workflows, findings)
  checkDockerfileBases(normalizedRoot, paths.dockerfiles, findings)
  checkComposeImages(normalizedRoot, paths.composeFiles, findings)
  checkWorkflowDockerImages(normalizedRoot, paths.workflows, findings)
  checkWorkflowToolInstalls(normalizedRoot, paths.workflows, findings)
  checkRequirementPins(normalizedRoot, paths.requirementsFiles, findings)
  checkPipInstallRequireHashes(normalizedRoot, paths.pipInstallFiles, findings)

  return findings
}

export const validateDockerDigestEvidence = (evidence) => {
  const findings = []
  const records = evidence?.records

  if (Array.isArray(evidence)) {
    addEvidenceFinding(findings, 'Evidence JSON must be an object produced by the resolver, not a bare records array.')
    return findings
  }

  if (!Array.isArray(records)) {
    addEvidenceFinding(findings, 'Evidence JSON must be an object with a records array.')
    return findings
  }

  if (evidence.schemaVersion !== 1) {
    addEvidenceFinding(findings, 'Evidence schemaVersion must be 1.')
  }
  if (evidence.resolver !== 'scripts/resolve-docker-image-digests.mjs') {
    addEvidenceFinding(findings, 'Evidence resolver must be scripts/resolve-docker-image-digests.mjs.')
  }
  if (evidence.digestAlgorithm !== 'sha256') {
    addEvidenceFinding(findings, 'Evidence digestAlgorithm must be sha256.')
  }

  const generatedAt = Date.parse(evidence.generatedAt)
  if (!evidence.generatedAt || Number.isNaN(generatedAt)) {
    addEvidenceFinding(findings, 'Evidence generatedAt must be an ISO timestamp from the networked resolver run.')
  }
  if (!evidence.verifier || typeof evidence.verifier !== 'string' || !evidence.verifier.trim()) {
    addEvidenceFinding(findings, 'Evidence verifier must identify the release operator who verified the digests.')
  }
  if (!evidence.lookupSource || typeof evidence.lookupSource !== 'string' || !evidence.lookupSource.trim()) {
    addEvidenceFinding(findings, 'Evidence lookupSource must describe the registry/API used for digest lookup.')
  }
  if (!Array.isArray(evidence.targetPlatforms) || evidence.targetPlatforms.length === 0 ||
    evidence.targetPlatforms.some((platform) => typeof platform !== 'string' || !platform.trim())) {
    addEvidenceFinding(findings, 'Evidence targetPlatforms must include at least one target platform or manifest scope.')
  }
  if (evidence.registry && evidence.registry !== 'registry-1.docker.io') {
    addEvidenceFinding(findings, `Evidence registry must be registry-1.docker.io, got ${evidence.registry}.`)
  }

  const bySourceAndImage = new Map()
  const digestByImage = new Map()
  for (const [index, record] of records.entries()) {
    const label = record?.source && record?.image ? `${record.source} ${record.image}` : `record #${index + 1}`
    if (!record || typeof record !== 'object') {
      addEvidenceFinding(findings, `${label} must be an object.`)
      continue
    }
    if (!record.source || !record.image) {
      addEvidenceFinding(findings, `${label} must include source and image.`)
      continue
    }
    if (!record.digest || !IMAGE_DIGEST_PATTERN.test(`image@${record.digest}`)) {
      addEvidenceFinding(findings, `${label} must include a sha256:<64-hex> digest.`)
      continue
    }
    const expectedPinned = `${record.image}@${record.digest.toLowerCase()}`
    if (record.pinned !== expectedPinned) {
      addEvidenceFinding(findings, `${label} pinned must equal ${expectedPinned}.`)
    }
    const mapKey = `${record.source}\0${record.image}`
    if (bySourceAndImage.has(mapKey)) {
      addEvidenceFinding(findings, `${label} appears more than once in evidence.`)
    }
    bySourceAndImage.set(mapKey, record)

    const lowerDigest = record.digest.toLowerCase()
    if (digestByImage.has(record.image) && digestByImage.get(record.image) !== lowerDigest) {
      addEvidenceFinding(findings, `${record.image} has conflicting digests in evidence.`)
    }
    digestByImage.set(record.image, lowerDigest)
  }

  for (const item of REQUIRED_IMAGE_REFS) {
    const key = `${item.source}\0${item.image}`
    if (!bySourceAndImage.has(key)) {
      addEvidenceFinding(findings, `Missing evidence for ${item.source} ${item.image}.`)
    }
  }

  return findings
}

export const loadDockerDigestEvidenceFindings = (evidencePath) => {
  try {
    const source = readFileSync(evidencePath, 'utf8')
    return validateDockerDigestEvidence(JSON.parse(source))
  } catch (error) {
    const findings = []
    addEvidenceFinding(findings, `Unable to read or parse evidence file ${evidencePath}: ${error.message}`)
    return findings
  }
}

export const formatSupplyChainPinFindings = (findings) => {
  const byKind = new Map()
  for (const finding of findings) {
    if (!byKind.has(finding.kind)) {
      byKind.set(finding.kind, [])
    }
    byKind.get(finding.kind).push(finding)
  }

  const output = []
  for (const [kind, items] of byKind) {
    output.push(`::error title=Supply chain pinning::${FINDING_TITLES[kind]}`)
    for (const item of items) {
      output.push(`${normalizePath(item.file)}:${item.line}: ${item.content}`)
    }
    output.push('')
  }

  if (findings.length > 0) {
    output.push('Supply-chain release pins are incomplete.')
    output.push('')
    output.push('This gate is intentionally fail-closed until operators replace every')
    output.push('mutable external reference with a verified GitHub Actions SHA, image')
    output.push('digest, or pip hash. See docs/release-supply-chain-pin-checklist.md')
    output.push('and docs/deployment-configuration.md for the manual pin list and')
    output.push('acceptance criteria.')
  }

  return output.join('\n')
}

export const runSupplyChainPinGate = (args = process.argv.slice(2)) => {
  const evidenceOnly = args.includes('--evidence-only')
  const evidenceIndex = args.findIndex((arg) => arg === '--evidence' || arg.startsWith('--evidence='))
  const evidencePath =
    evidenceIndex >= 0
      ? (args[evidenceIndex].includes('=') ? args[evidenceIndex].split('=')[1] : args[evidenceIndex + 1])
      : evidenceOnly ? null : DEFAULT_DOCKER_DIGEST_EVIDENCE_PATH
  const repoArg = args.find((arg, index) =>
    !arg.startsWith('--') &&
    !(evidenceIndex >= 0 && index === evidenceIndex + 1 && !args[evidenceIndex].includes('='))
  )
  const repoRoot = repoArg ? path.resolve(repoArg) : path.resolve(path.dirname(scriptPath), '..')
  const findings = evidenceOnly ? [] : collectSupplyChainPinFindings(repoRoot)
  if (evidenceOnly && !evidencePath) {
    findings.push(...validateDockerDigestEvidence(null))
  }
  if (evidencePath) {
    findings.push(...loadDockerDigestEvidenceFindings(path.resolve(repoRoot, evidencePath)))
  }
  const output = formatSupplyChainPinFindings(findings)
  return {
    exitCode: findings.length > 0 ? 1 : 0,
    findings,
    output: output || 'Supply-chain release pins are complete.'
  }
}

const main = () => {
  const result = runSupplyChainPinGate()
  const output = result.output
  if (output) {
    if (result.exitCode > 0) {
      console.error(output)
    } else {
      console.log(output)
    }
  } else {
    console.log('Supply-chain release pins are complete.')
  }
  process.exitCode = result.exitCode
}

if (process.argv[1] && pathToFileURL(path.resolve(process.argv[1])).href === import.meta.url) {
  main()
}
