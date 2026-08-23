import { randomUUID } from 'node:crypto'
import { open, rename, rm } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const ACCEPT_MANIFESTS = [
  'application/vnd.oci.image.index.v1+json',
  'application/vnd.docker.distribution.manifest.list.v2+json',
  'application/vnd.oci.image.manifest.v1+json',
  'application/vnd.docker.distribution.manifest.v2+json'
].join(', ')

const DIGEST_PATTERN = /^sha256:[0-9a-f]{64}$/i
const scriptPath = fileURLToPath(import.meta.url)

export const REQUIRED_IMAGE_REFS = [
  { source: 'backend/Dockerfile:2', image: 'maven:3.9-eclipse-temurin-17' },
  { source: 'backend/Dockerfile:9', image: 'eclipse-temurin:17-jre-alpine' },
  { source: 'frontend/Dockerfile:2', image: 'node:22-alpine' },
  { source: 'frontend/Dockerfile:28', image: 'nginx:alpine' },
  { source: 'scrapler/Dockerfile:1', image: 'python:3.11-slim' },
  { source: 'docker-compose.yml:3', image: 'mysql:8.4' },
  { source: 'docker-compose.yml:126', image: 'redis:7-alpine' },
  { source: 'docker-compose.prod.yml:12', image: 'alpine:3.21' },
  { source: 'docker-compose.prod.yml:532', image: 'redis:7-alpine' },
  { source: 'docker-compose.prod.yml:577', image: 'mysql:8.4' },
  { source: 'docker-compose.prod.yml:717', image: 'prom/prometheus:v2.55.1' },
  { source: 'docker-compose.prod.yml:758', image: 'prom/alertmanager:v0.27.0' },
  { source: 'docker-compose.prod.yml:805', image: 'grafana/grafana:11.4.0' },
  { source: 'docker-compose.prod.yml:840', image: 'openzipkin/zipkin:3.4' },
  { source: '.github/workflows/ci.yml:138', image: 'prom/prometheus:v2.55.1' },
  { source: '.github/workflows/ci.yml:143', image: 'prom/prometheus:v2.55.1' },
  { source: '.github/workflows/ci.yml:261', image: 'mysql:8.4' }
]

export const splitDockerImageRef = (imageRef) => {
  const withoutDigest = imageRef.split('@')[0]
  const lastSlash = withoutDigest.lastIndexOf('/')
  const lastColon = withoutDigest.lastIndexOf(':')
  if (lastColon <= lastSlash) {
    throw new Error(`Image reference must include an explicit tag: ${imageRef}`)
  }

  return {
    name: withoutDigest.slice(0, lastColon),
    tag: withoutDigest.slice(lastColon + 1)
  }
}

export const dockerHubRepository = (imageName) => {
  const firstSegment = imageName.split('/')[0]
  if (firstSegment.includes('.') || firstSegment.includes(':') || firstSegment === 'localhost') {
    throw new Error(`Only Docker Hub image references are supported: ${imageName}`)
  }

  return imageName.includes('/') ? imageName : `library/${imageName}`
}

export const dockerHubTokenUrl = (repository) =>
  `https://auth.docker.io/token?service=registry.docker.io&scope=repository:${repository}:pull`

export const dockerRegistryManifestUrl = (repository, tag) =>
  `https://registry-1.docker.io/v2/${repository}/manifests/${encodeURIComponent(tag)}`

const responseText = async (response) => {
  try {
    return await response.text()
  } catch {
    return ''
  }
}

const fetchWithTimeout = async (fetchImpl, url, options = {}, timeoutMs = 10_000) => {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), timeoutMs)
  try {
    return await fetchImpl(url, {
      ...options,
      signal: options.signal ?? controller.signal
    })
  } finally {
    clearTimeout(timeout)
  }
}

const requireOk = async (response, label) => {
  if (response.ok) return

  const detail = await responseText(response)
  throw new Error(`${label} failed: HTTP ${response.status}${detail ? ` ${detail.slice(0, 180)}` : ''}`)
}

export const resolveDockerHubDigest = async (
  imageRef,
  { fetchImpl = globalThis.fetch, timeoutMs = 10_000 } = {}
) => {
  if (!fetchImpl) {
    throw new Error('Global fetch is unavailable; run with Node.js 18 or newer.')
  }

  const { name, tag } = splitDockerImageRef(imageRef)
  const repository = dockerHubRepository(name)
  const tokenResponse = await fetchWithTimeout(fetchImpl, dockerHubTokenUrl(repository), {}, timeoutMs)
  await requireOk(tokenResponse, `Docker Hub token request for ${imageRef}`)

  const tokenBody = await tokenResponse.json()
  if (!tokenBody?.token) {
    throw new Error(`Docker Hub token response did not include a token for ${imageRef}`)
  }

  const manifestUrl = dockerRegistryManifestUrl(repository, tag)
  const headers = {
    Accept: ACCEPT_MANIFESTS,
    Authorization: `Bearer ${tokenBody.token}`
  }

  let manifestResponse = await fetchWithTimeout(fetchImpl, manifestUrl, { method: 'HEAD', headers }, timeoutMs)
  if (manifestResponse.status === 405) {
    manifestResponse = await fetchWithTimeout(fetchImpl, manifestUrl, { method: 'GET', headers }, timeoutMs)
  }
  await requireOk(manifestResponse, `Docker manifest request for ${imageRef}`)

  const digest = manifestResponse.headers.get('docker-content-digest')
  if (!digest || !DIGEST_PATTERN.test(digest)) {
    throw new Error(`Docker manifest response did not include a valid sha256 digest for ${imageRef}`)
  }

  return {
    image: imageRef,
    pinned: `${imageRef}@${digest.toLowerCase()}`,
    repository,
    tag,
    digest: digest.toLowerCase()
  }
}

const formatMarkdown = (records) => [
  '| Source | Image | Pinned reference |',
  '| --- | --- | --- |',
  ...records.map((record) => `| \`${record.source}\` | \`${record.image}\` | \`${record.pinned}\` |`)
].join('\n')

const formatText = (records) =>
  records.map((record) => `${record.source} ${record.pinned}`).join('\n')

export const createDigestEvidence = (
  records,
  {
    generatedAt = new Date().toISOString(),
    resolver = 'scripts/resolve-docker-image-digests.mjs',
    verifier = '',
    targetPlatforms = ['multi-platform-index'],
    lookupSource = 'Docker Registry HTTP API v2 via registry-1.docker.io'
  } = {}
) => ({
  schemaVersion: 1,
  generatedAt,
  resolver,
  verifier,
  targetPlatforms,
  lookupSource,
  registry: 'registry-1.docker.io',
  digestAlgorithm: 'sha256',
  records: records.map((record) => ({
    source: record.source,
    image: record.image,
    pinned: record.pinned,
    repository: record.repository,
    tag: record.tag,
    digest: record.digest
  }))
})

export const resolveRequiredImageDigests = async (
  requiredImageRefs = REQUIRED_IMAGE_REFS,
  { resolveImpl = resolveDockerHubDigest, timeoutMs = 10_000 } = {}
) => {
  const records = []
  const resolved = new Map()

  for (const item of requiredImageRefs) {
    try {
      if (!resolved.has(item.image)) {
        resolved.set(item.image, await resolveImpl(item.image, { timeoutMs }))
      }
      records.push({
        source: item.source,
        ...resolved.get(item.image)
      })
    } catch (error) {
      throw new Error(`Failed to resolve ${item.image} (${item.source}): ${error.message}`, {
        cause: error
      })
    }
  }

  return records
}

export const writeOutputAtomically = async (outputPath, content) => {
  const targetPath = path.resolve(outputPath)
  const temporaryPath = path.join(
    path.dirname(targetPath),
    `.${path.basename(targetPath)}.${process.pid}.${randomUUID()}.tmp`
  )
  let outputHandle

  try {
    outputHandle = await open(temporaryPath, 'wx', 0o644)
    await outputHandle.writeFile(content, 'utf8')
    await outputHandle.sync()
    await outputHandle.close()
    outputHandle = undefined
    await rename(temporaryPath, targetPath)
  } finally {
    if (outputHandle) {
      await outputHandle.close().catch(() => {})
    }
    await rm(temporaryPath, { force: true }).catch(() => {})
  }
}

export const parseCliArgs = (args) => {
  const options = {
    json: false,
    evidence: false,
    markdown: false,
    timeoutMs: 10_000,
    verifier: '',
    targetPlatforms: [],
    outputPath: ''
  }
  const seen = new Set()
  const markOnce = (name) => {
    if (seen.has(name)) {
      throw new Error(`Argument may be provided only once: ${name}`)
    }
    seen.add(name)
  }

  for (const arg of args) {
    if (arg === '--json' || arg === '--evidence' || arg === '--markdown') {
      markOnce(arg)
      options[arg.slice(2)] = true
      continue
    }

    if (arg.startsWith('--timeout-ms=')) {
      markOnce('--timeout-ms')
      const value = arg.slice('--timeout-ms='.length)
      if (!/^[1-9][0-9]*$/.test(value)) {
        throw new Error('--timeout-ms must be a positive integer.')
      }
      options.timeoutMs = Number.parseInt(value, 10)
      continue
    }

    if (arg.startsWith('--verifier=')) {
      markOnce('--verifier')
      options.verifier = arg.slice('--verifier='.length)
      continue
    }

    if (arg.startsWith('--target-platform=')) {
      const value = arg.slice('--target-platform='.length).trim()
      if (!value) {
        throw new Error('--target-platform must not be empty.')
      }
      if (options.targetPlatforms.includes(value)) {
        throw new Error(`Duplicate target platform: ${value}`)
      }
      options.targetPlatforms.push(value)
      continue
    }

    if (arg.startsWith('--output=')) {
      markOnce('--output')
      options.outputPath = arg.slice('--output='.length)
      if (!options.outputPath.trim()) {
        throw new Error('Atomic output requires a non-empty --output=<path>.')
      }
      continue
    }

    throw new Error(`Unknown argument: ${arg}`)
  }

  const formats = [
    options.json && '--json',
    options.evidence && '--evidence',
    options.markdown && '--markdown'
  ].filter(Boolean)
  if (formats.length > 1) {
    throw new Error(`Output format arguments are mutually exclusive: ${formats.join(', ')}`)
  }

  return options
}

const main = async () => {
  let options
  try {
    options = parseCliArgs(process.argv.slice(2))
  } catch (error) {
    console.error(error.message)
    process.exitCode = 1
    return
  }

  const {
    json,
    evidence,
    markdown,
    timeoutMs,
    verifier,
    targetPlatforms,
    outputPath
  } = options

  if (evidence && !verifier.trim()) {
    console.error('Evidence output requires --verifier=<release-operator> so the release record is accountable.')
    process.exitCode = 1
    return
  }

  let records
  try {
    records = await resolveRequiredImageDigests(REQUIRED_IMAGE_REFS, { timeoutMs })
  } catch (error) {
    console.error(error.message)
    process.exitCode = 1
    return
  }

  let output
  if (evidence) {
    output = JSON.stringify(createDigestEvidence(records, {
      verifier,
      targetPlatforms: targetPlatforms.length > 0 ? targetPlatforms : ['multi-platform-index']
    }), null, 2)
  } else if (json) {
    output = JSON.stringify(records, null, 2)
  } else if (markdown) {
    output = formatMarkdown(records)
  } else {
    output = formatText(records)
  }

  if (outputPath) {
    try {
      await writeOutputAtomically(outputPath, `${output}\n`)
      console.error(`Wrote Docker digest output atomically to ${path.resolve(outputPath)}`)
    } catch (error) {
      console.error(`Failed to write Docker digest output to ${path.resolve(outputPath)}: ${error.message}`)
      process.exitCode = 1
    }
  } else {
    console.log(output)
  }
}

if (process.argv[1] && path.resolve(process.argv[1]) === scriptPath) {
  await main()
}
