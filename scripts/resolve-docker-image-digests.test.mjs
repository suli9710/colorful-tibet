import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import {
  mkdtempSync,
  readFileSync,
  readdirSync,
  rmSync,
  writeFileSync
} from 'node:fs'
import { spawnSync } from 'node:child_process'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  createDigestEvidence,
  dockerHubRepository,
  dockerHubTokenUrl,
  dockerRegistryManifestUrl,
  parseCliArgs,
  REQUIRED_IMAGE_REFS,
  resolveDockerHubDigest,
  resolveRequiredImageDigests,
  splitDockerImageRef,
  writeOutputAtomically
} from './resolve-docker-image-digests.mjs'

const digest = `sha256:${'a'.repeat(64)}`
const resolverScript = fileURLToPath(new URL('./resolve-docker-image-digests.mjs', import.meta.url))
const pinnedImageFiles = [
  'backend/Dockerfile',
  'frontend/Dockerfile',
  'scrapler/Dockerfile',
  'docker-compose.yml',
  'docker-compose.prod.yml',
  '.github/workflows/ci.yml'
]

describe('Docker image digest resolver', () => {
  it('enumerates every digest-pinned image reference in the repository', () => {
    const discovered = []
    for (const file of pinnedImageFiles) {
      const lines = readFileSync(file, 'utf8').split(/\r?\n/)
      lines.forEach((line, index) => {
        const matches = line.matchAll(/([A-Za-z0-9._/-]+:[A-Za-z0-9._-]+)@sha256:[0-9a-f]{64}/gi)
        for (const match of matches) {
          discovered.push(`${file}:${index + 1}\u0000${match[1]}`)
        }
      })
    }

    const required = REQUIRED_IMAGE_REFS.map((item) => `${item.source}\u0000${item.image}`)
    assert.deepEqual(discovered.sort(), required.sort())
  })

  it('keeps required image source labels aligned with current repository files', () => {
    for (const item of REQUIRED_IMAGE_REFS) {
      const separator = item.source.lastIndexOf(':')
      const file = item.source.slice(0, separator)
      const lineNumber = Number.parseInt(item.source.slice(separator + 1), 10)
      const line = readFileSync(file, 'utf8').split(/\r?\n/)[lineNumber - 1]

      assert.ok(line?.includes(item.image), `${item.source} should contain ${item.image}`)
    }
  })

  it('keeps the recorded release evidence equal to the digests actually pinned in the repo', () => {
    // Matching only the untagged image name let a Dockerfile whose @sha256: disagreed with the
    // recorded evidence pass CI, which is exactly the drift this evidence file exists to prevent.
    const evidence = JSON.parse(readFileSync('docker-digest-evidence.json', 'utf8'))
    const recordsBySourceAndImage = new Map(
      evidence.records.map((record) => [`${record.source}\u0000${record.image}`, record])
    )

    for (const item of REQUIRED_IMAGE_REFS) {
      const record = recordsBySourceAndImage.get(`${item.source}\u0000${item.image}`)
      assert.ok(record, `docker-digest-evidence.json is missing ${item.source} ${item.image}`)

      const separator = item.source.lastIndexOf(':')
      const file = item.source.slice(0, separator)
      const lineNumber = Number.parseInt(item.source.slice(separator + 1), 10)
      const line = readFileSync(file, 'utf8').split(/\r?\n/)[lineNumber - 1]
      const pinned = line.slice(line.indexOf(item.image) + item.image.length)
        .match(/^@(sha256:[0-9a-f]{64})/i)

      assert.ok(pinned, `${item.source} must pin ${item.image} by sha256 digest`)
      assert.equal(
        pinned[1].toLowerCase(),
        record.digest.toLowerCase(),
        `${item.source} pins ${pinned[1]} but the release evidence records ${record.digest}`
      )
    }
  })

  it('normalizes Docker Hub official and namespaced image references', () => {
    assert.deepEqual(splitDockerImageRef('alpine:3.21'), {
      name: 'alpine',
      tag: '3.21'
    })
    assert.deepEqual(splitDockerImageRef('prom/prometheus:v2.55.1'), {
      name: 'prom/prometheus',
      tag: 'v2.55.1'
    })
    assert.equal(dockerHubRepository('alpine'), 'library/alpine')
    assert.equal(dockerHubRepository('grafana/grafana'), 'grafana/grafana')
  })

  it('rejects image refs without explicit tags or outside Docker Hub', () => {
    assert.throws(() => splitDockerImageRef('alpine'), /explicit tag/)
    assert.throws(() => dockerHubRepository('ghcr.io/example/app'), /Only Docker Hub/)
  })

  it('rejects unknown, duplicate, conflicting, or incomplete CLI arguments', () => {
    assert.throws(() => parseCliArgs(['--evidnce']), /Unknown argument/)
    assert.throws(() => parseCliArgs(['--json', '--evidence']), /mutually exclusive/)
    assert.throws(() => parseCliArgs(['--output=one', '--output=two']), /only once/)
    assert.throws(() => parseCliArgs(['--target-platform=']), /must not be empty/)
    assert.throws(
      () => parseCliArgs(['--target-platform=linux/amd64', '--target-platform=linux/amd64']),
      /Duplicate target platform/
    )
    assert.throws(() => parseCliArgs(['--timeout-ms=0']), /positive integer/)
  })

  it('fails unsafe CLI input before changing an existing output file', () => {
    const directory = mkdtempSync(path.join(tmpdir(), 'docker-digest-cli-'))
    const outputPath = path.join(directory, 'evidence.json')

    try {
      writeFileSync(outputPath, 'trusted evidence\n', 'utf8')
      const result = spawnSync(process.execPath, [
        resolverScript,
        '--evidnce',
        `--output=${outputPath}`
      ], {
        encoding: 'utf8'
      })

      assert.notEqual(result.status, 0)
      assert.equal(result.stdout, '')
      assert.match(result.stderr, /Unknown argument: --evidnce/)
      assert.equal(readFileSync(outputPath, 'utf8'), 'trusted evidence\n')
      assert.deepEqual(readdirSync(directory), ['evidence.json'])
    } finally {
      rmSync(directory, { recursive: true, force: true })
    }
  })

  it('resolves the registry manifest digest with a bearer token', async () => {
    const calls = []
    const fetchImpl = async (url, options = {}) => {
      calls.push({ url, options })
      if (url === dockerHubTokenUrl('library/alpine')) {
        return new Response(JSON.stringify({ token: 'test-token' }), { status: 200 })
      }
      if (url === dockerRegistryManifestUrl('library/alpine', '3.21')) {
        return new Response('', {
          status: 200,
          headers: { 'docker-content-digest': digest }
        })
      }
      throw new Error(`Unexpected URL: ${url}`)
    }

    const result = await resolveDockerHubDigest('alpine:3.21', { fetchImpl })

    assert.equal(result.digest, digest)
    assert.equal(result.pinned, `alpine:3.21@${digest}`)
    assert.equal(calls[1].options.method, 'HEAD')
    assert.equal(calls[1].options.headers.Authorization, 'Bearer test-token')
    assert.match(calls[1].options.headers.Accept, /manifest/)
  })

  it('falls back to GET when a registry does not allow HEAD', async () => {
    let manifestCalls = 0
    const fetchImpl = async (url) => {
      if (url === dockerHubTokenUrl('prom/prometheus')) {
        return new Response(JSON.stringify({ token: 'test-token' }), { status: 200 })
      }
      if (url === dockerRegistryManifestUrl('prom/prometheus', 'v2.55.1')) {
        manifestCalls += 1
        return new Response('', {
          status: manifestCalls === 1 ? 405 : 200,
          headers: manifestCalls === 1 ? {} : { 'docker-content-digest': digest }
        })
      }
      throw new Error(`Unexpected URL: ${url}`)
    }

    const result = await resolveDockerHubDigest('prom/prometheus:v2.55.1', { fetchImpl })

    assert.equal(result.pinned, `prom/prometheus:v2.55.1@${digest}`)
    assert.equal(manifestCalls, 2)
  })

  it('fails closed when the registry does not return a digest', async () => {
    const fetchImpl = async (url) => {
      if (url === dockerHubTokenUrl('library/nginx')) {
        return new Response(JSON.stringify({ token: 'test-token' }), { status: 200 })
      }
      if (url === dockerRegistryManifestUrl('library/nginx', 'alpine')) {
        return new Response('', { status: 200 })
      }
      throw new Error(`Unexpected URL: ${url}`)
    }

    await assert.rejects(
      () => resolveDockerHubDigest('nginx:alpine', { fetchImpl }),
      /valid sha256 digest/
    )
  })

  it('returns no partial result when any required digest resolution fails', async () => {
    const required = [
      { source: 'first/Dockerfile:1', image: 'alpine:3.21' },
      { source: 'second/Dockerfile:1', image: 'nginx:alpine' }
    ]
    const resolveImpl = async (image) => {
      if (image === 'nginx:alpine') {
        throw new Error('registry unavailable')
      }
      return {
        image,
        pinned: `${image}@${digest}`,
        repository: 'library/alpine',
        tag: '3.21',
        digest
      }
    }

    await assert.rejects(
      () => resolveRequiredImageDigests(required, { resolveImpl }),
      /Failed to resolve nginx:alpine .*registry unavailable/
    )
  })

  it('replaces an existing output through a temporary file without leaving artifacts', async () => {
    const directory = mkdtempSync(path.join(tmpdir(), 'docker-digest-output-'))
    const outputPath = path.join(directory, 'evidence.json')

    try {
      writeFileSync(outputPath, 'old evidence\n', 'utf8')
      await writeOutputAtomically(outputPath, 'new evidence\n')

      assert.equal(readFileSync(outputPath, 'utf8'), 'new evidence\n')
      assert.deepEqual(readdirSync(directory), ['evidence.json'])
    } finally {
      rmSync(directory, { recursive: true, force: true })
    }
  })

  it('formats resolver output as auditable evidence', () => {
    const evidence = createDigestEvidence([
      {
        source: 'docker-compose.prod.yml:6',
        image: 'alpine:3.21',
        pinned: `alpine:3.21@${digest}`,
        repository: 'library/alpine',
        tag: '3.21',
        digest
      }
    ], {
      generatedAt: '2026-06-09T00:00:00.000Z',
      verifier: 'release-operator@example.test',
      targetPlatforms: ['linux/amd64']
    })

    assert.equal(evidence.schemaVersion, 1)
    assert.equal(evidence.generatedAt, '2026-06-09T00:00:00.000Z')
    assert.equal(evidence.verifier, 'release-operator@example.test')
    assert.deepEqual(evidence.targetPlatforms, ['linux/amd64'])
    assert.match(evidence.lookupSource, /Docker Registry/)
    assert.equal(evidence.registry, 'registry-1.docker.io')
    assert.deepEqual(evidence.records[0], {
      source: 'docker-compose.prod.yml:6',
      image: 'alpine:3.21',
      pinned: `alpine:3.21@${digest}`,
      repository: 'library/alpine',
      tag: '3.21',
      digest
    })
  })
})
