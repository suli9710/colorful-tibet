import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { afterEach, describe, it } from 'node:test'
import assert from 'node:assert/strict'
import {
  collectSupplyChainPinFindings,
  validateDockerDigestEvidence
} from './check-supply-chain-pins.mjs'
import { REQUIRED_IMAGE_REFS } from './resolve-docker-image-digests.mjs'

let tempRoots = []

const createFixture = (files) => {
  const root = mkdtempSync(path.join(tmpdir(), 'ct-supply-chain-'))
  tempRoots.push(root)
  for (const [relativePath, content] of Object.entries(files)) {
    const fullPath = path.join(root, relativePath)
    mkdirSync(path.dirname(fullPath), { recursive: true })
    writeFileSync(fullPath, content)
  }
  return root
}

const completeEvidenceRecords = () => {
  const digestByImage = new Map()
  return REQUIRED_IMAGE_REFS.map((item, index) => {
    if (!digestByImage.has(item.image)) {
      digestByImage.set(item.image, `sha256:${String(index).padStart(64, 'a')}`)
    }
    const digest = digestByImage.get(item.image)
    return {
      source: item.source,
      image: item.image,
      pinned: `${item.image}@${digest}`,
      repository: item.image.includes('/') ? item.image.split(':')[0] : `library/${item.image.split(':')[0]}`,
      tag: item.image.split(':').at(-1),
      digest
    }
  })
}

const completeEvidence = () => ({
  schemaVersion: 1,
  generatedAt: '2026-06-09T00:00:00.000Z',
  verifier: 'release-operator@example.test',
  targetPlatforms: ['multi-platform-index'],
  lookupSource: 'Docker Registry HTTP API v2 via registry-1.docker.io',
  registry: 'registry-1.docker.io',
  records: completeEvidenceRecords()
})

afterEach(() => {
  for (const root of tempRoots) {
    rmSync(root, { recursive: true, force: true })
  }
  tempRoots = []
})

describe('supply-chain pin gate', () => {
  it('fails closed for mutable action refs, image tags, range pins, missing hashes, and non-hash installs', () => {
    const root = createFixture({
      '.github/workflows/ci.yml': [
        'jobs:',
        '  test:',
        '    steps:',
        '      - uses: actions/checkout@v4',
        '      - run: |',
        '          go install github.com/rhysd/actionlint/cmd/actionlint@v1.7.8',
        '          python -m pip install yamllint==1.35.1',
        '          docker run --rm \\',
        '            prom/prometheus:v2.55.1 \\',
        '            promtool check config /etc/prometheus/prometheus.yml',
        '          docker run --rm redis:7-alpine redis-cli --version',
        '          python -m pip install -r requirements-dev.txt'
      ].join('\n'),
      'backend/Dockerfile': 'FROM maven:3.9-eclipse-temurin-17 AS builder\nFROM eclipse-temurin:17-jre-alpine\n',
      'frontend/Dockerfile': 'FROM node:22-alpine AS builder\nFROM nginx:alpine\n',
      'scrapler/Dockerfile': 'FROM python:3.11-slim\nRUN pip install --no-cache-dir -r requirements.txt\n',
      'docker-compose.yml': 'services:\n  mysql:\n    image: mysql:8.4\n',
      'docker-compose.prod.yml': 'services:\n  redis:\n    image: redis:7-alpine\n',
      'scrapler/requirements.txt': 'fastapi==0.136.1\n',
      'scrapler/requirements-dev.txt': '-r requirements.txt\npytest>=8.3,<9.0\n'
    })

    const kinds = new Set(collectSupplyChainPinFindings(root).map((finding) => finding.kind))

    assert.deepEqual(kinds, new Set([
      'actionSha',
      'dockerfileDigest',
      'composeDigest',
      'workflowImageDigest',
      'workflowToolInstall',
      'pythonExactPin',
      'pythonHash',
      'pipRequireHashes'
    ]))
  })

  it('accepts operator-supplied full SHAs, image digests, requirement hashes, and hash-enforced installs', () => {
    const sha = '0123456789abcdef0123456789abcdef01234567'
    const digest = 'a'.repeat(64)
    const hash = 'b'.repeat(64)
    const root = createFixture({
      '.github/workflows/ci.yml': [
        'jobs:',
        '  test:',
        '    steps:',
        `      - uses: actions/checkout@${sha}`,
        '      - run: |',
        '          docker run --rm \\',
        `            prom/prometheus:v2.55.1@sha256:${digest} \\`,
        '            promtool check config /etc/prometheus/prometheus.yml',
        `          docker run --rm redis:7-alpine@sha256:${digest} redis-cli --version`,
        '          python -m pip install --require-hashes -r requirements-dev.txt'
      ].join('\n'),
      'backend/Dockerfile': `FROM maven:3.9-eclipse-temurin-17@sha256:${digest} AS builder\nFROM eclipse-temurin:17-jre-alpine@sha256:${digest}\n`,
      'frontend/Dockerfile': `FROM node:22-alpine@sha256:${digest} AS builder\nFROM nginx:alpine@sha256:${digest}\n`,
      'scrapler/Dockerfile': `FROM python:3.11-slim@sha256:${digest}\nRUN pip install --no-cache-dir --require-hashes -r requirements.txt\n`,
      'docker-compose.yml': `services:\n  mysql:\n    image: mysql:8.4@sha256:${digest}\n`,
      'docker-compose.prod.yml': `services:\n  redis:\n    image: redis:7-alpine@sha256:${digest}\n`,
      'scrapler/requirements.txt': `fastapi==0.136.1 \\\n    --hash=sha256:${hash}\n`,
      'scrapler/requirements-dev.txt': `-r requirements.txt\npytest==8.3.5 --hash=sha256:${hash}\n`
    })

    assert.deepEqual(collectSupplyChainPinFindings(root), [])
  })

  it('accepts complete offline Docker digest evidence from the resolver', () => {
    assert.deepEqual(validateDockerDigestEvidence(completeEvidence()), [])
  })

  it('rejects bare Docker digest record arrays without resolver metadata', () => {
    const findings = validateDockerDigestEvidence(completeEvidenceRecords())

    assert.ok(findings.some((finding) =>
      finding.content.includes('object produced by the resolver')))
  })

  it('supports an evidence-only CLI audit without requiring pinned repo files yet', () => {
    const root = createFixture({
      'docker-digest-evidence.json': JSON.stringify(completeEvidence())
    })

    const result = spawnSync(process.execPath, [
      path.join(process.cwd(), 'scripts/check-supply-chain-pins.mjs'),
      '--evidence-only',
      '--evidence',
      'docker-digest-evidence.json',
      root
    ], { encoding: 'utf8' })

    assert.equal(result.status, 0, result.stderr)
    assert.match(result.stdout, /Supply-chain release pins are complete/)
  })

  it('rejects incomplete or tampered Docker digest evidence without registry access', () => {
    const findings = validateDockerDigestEvidence({
      schemaVersion: 1,
      generatedAt: 'not-a-date',
      verifier: '',
      targetPlatforms: [],
      lookupSource: '',
      registry: 'registry.example.invalid',
      records: [
        {
          source: REQUIRED_IMAGE_REFS[0].source,
          image: REQUIRED_IMAGE_REFS[0].image,
          pinned: `${REQUIRED_IMAGE_REFS[0].image}@sha256:${'b'.repeat(64)}`,
          digest: `sha256:${'c'.repeat(64)}`
        },
        {
          source: REQUIRED_IMAGE_REFS[0].source,
          image: REQUIRED_IMAGE_REFS[0].image,
          pinned: `${REQUIRED_IMAGE_REFS[0].image}@sha256:${'c'.repeat(64)}`,
          digest: `sha256:${'c'.repeat(64)}`
        }
      ]
    })

    const messages = findings.map((finding) => finding.content)

    assert.ok(messages.some((message) => message.includes('generatedAt')))
    assert.ok(messages.some((message) => message.includes('verifier')))
    assert.ok(messages.some((message) => message.includes('lookupSource')))
    assert.ok(messages.some((message) => message.includes('targetPlatforms')))
    assert.ok(messages.some((message) => message.includes('registry-1.docker.io')))
    assert.ok(messages.some((message) => message.includes('pinned must equal')))
    assert.ok(messages.some((message) => message.includes('appears more than once')))
    assert.ok(messages.some((message) => message.includes(`Missing evidence for ${REQUIRED_IMAGE_REFS[1].source}`)))
  })
})
