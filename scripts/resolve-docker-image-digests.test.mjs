import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import {
  createDigestEvidence,
  dockerHubRepository,
  dockerHubTokenUrl,
  dockerRegistryManifestUrl,
  REQUIRED_IMAGE_REFS,
  resolveDockerHubDigest,
  splitDockerImageRef
} from './resolve-docker-image-digests.mjs'

const digest = `sha256:${'a'.repeat(64)}`

describe('Docker image digest resolver', () => {
  it('keeps required image source labels aligned with current repository files', () => {
    for (const item of REQUIRED_IMAGE_REFS) {
      const separator = item.source.lastIndexOf(':')
      const file = item.source.slice(0, separator)
      const lineNumber = Number.parseInt(item.source.slice(separator + 1), 10)
      const line = readFileSync(file, 'utf8').split(/\r?\n/)[lineNumber - 1]

      assert.ok(line?.includes(item.image), `${item.source} should contain ${item.image}`)
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
