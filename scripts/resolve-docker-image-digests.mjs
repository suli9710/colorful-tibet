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
  { source: 'frontend/Dockerfile:24', image: 'nginx:alpine' },
  { source: 'scrapler/Dockerfile:1', image: 'python:3.11-slim' },
  { source: 'docker-compose.yml:3', image: 'mysql:8.4' },
  { source: 'docker-compose.yml:117', image: 'redis:7-alpine' },
  { source: 'docker-compose.prod.yml:6', image: 'alpine:3.21' },
  { source: 'docker-compose.prod.yml:206', image: 'redis:7-alpine' },
  { source: 'docker-compose.prod.yml:242', image: 'mysql:8.4' },
  { source: 'docker-compose.prod.yml:370', image: 'prom/prometheus:v2.55.1' },
  { source: 'docker-compose.prod.yml:394', image: 'prom/alertmanager:v0.27.0' },
  { source: 'docker-compose.prod.yml:432', image: 'grafana/grafana:11.4.0' },
  { source: 'docker-compose.prod.yml:458', image: 'openzipkin/zipkin:3.4' },
  { source: '.github/workflows/ci.yml:126', image: 'prom/prometheus:v2.55.1' },
  { source: '.github/workflows/ci.yml:130', image: 'prom/prometheus:v2.55.1' }
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

const main = async () => {
  const json = process.argv.includes('--json')
  const evidence = process.argv.includes('--evidence')
  const markdown = process.argv.includes('--markdown')
  const timeoutArg = process.argv.find((arg) => arg.startsWith('--timeout-ms='))
  const verifierArg = process.argv.find((arg) => arg.startsWith('--verifier='))
  const platformArgs = process.argv
    .filter((arg) => arg.startsWith('--target-platform='))
    .map((arg) => arg.split('=')[1])
  const timeoutMs = timeoutArg ? Number.parseInt(timeoutArg.split('=')[1], 10) : 10_000
  const verifier = verifierArg ? verifierArg.split('=')[1] : ''
  const records = []
  const resolved = new Map()

  if (evidence && !verifier.trim()) {
    console.error('Evidence output requires --verifier=<release-operator> so the release record is accountable.')
    process.exitCode = 1
    return
  }

  for (const item of REQUIRED_IMAGE_REFS) {
    try {
      if (!resolved.has(item.image)) {
        resolved.set(item.image, await resolveDockerHubDigest(item.image, { timeoutMs }))
      }
      records.push({
        source: item.source,
        ...resolved.get(item.image)
      })
    } catch (error) {
      console.error(`Failed to resolve ${item.image} (${item.source}): ${error.message}`)
      process.exitCode = 1
    }
  }

  if (records.length > 0) {
    if (evidence) {
      console.log(JSON.stringify(createDigestEvidence(records, {
        verifier,
        targetPlatforms: platformArgs.length > 0 ? platformArgs : ['multi-platform-index']
      }), null, 2))
    } else if (json) {
      console.log(JSON.stringify(records, null, 2))
    } else if (markdown) {
      console.log(formatMarkdown(records))
    } else {
      console.log(formatText(records))
    }
  }
}

if (process.argv[1] && path.resolve(process.argv[1]) === scriptPath) {
  await main()
}
