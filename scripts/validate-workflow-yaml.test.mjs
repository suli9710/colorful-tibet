import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import { validateWorkflowDefinition } from './validate-workflow-yaml.mjs'

const validWorkflow = () => ({
  name: 'CI',
  on: {
    push: { branches: ['main', 'master'] },
    pull_request: null
  },
  permissions: {
    contents: 'read'
  },
  jobs: {
    'repository-noise': { steps: [] },
    'workflow-lint': {
      steps: [
        { run: 'npm --prefix frontend ci' },
        { run: 'node scripts/validate-workflow-yaml.mjs' }
      ]
    },
    'supply-chain-pin-gate': {
      steps: [{ run: 'node scripts/check-supply-chain-pins.mjs' }]
    },
    'secret-scan': { steps: [] },
    'deployment-config': { steps: [] },
    backend: { steps: [] },
    scrapler: { steps: [] },
    frontend: { steps: [] },
    'docker-images': { steps: [] }
  }
})

describe('workflow YAML validator', () => {
  it('accepts the required CI workflow shape', () => {
    assert.deepEqual(validateWorkflowDefinition(validWorkflow()), [])
  })

  it('rejects missing required jobs and weak permissions', () => {
    const workflow = validWorkflow()
    workflow.permissions.contents = 'write'
    delete workflow.jobs['supply-chain-pin-gate']

    const errors = validateWorkflowDefinition(workflow)

    assert(errors.some((error) => error.includes('contents: read')))
    assert(errors.some((error) => error.includes('supply-chain-pin-gate')))
  })

  it('requires the locked local workflow validation path', () => {
    const workflow = validWorkflow()
    workflow.jobs['workflow-lint'].steps = [{ run: 'go install github.com/rhysd/actionlint/cmd/actionlint@v1.7.8' }]

    const errors = validateWorkflowDefinition(workflow)

    assert(errors.some((error) => error.includes('scripts/validate-workflow-yaml.mjs')))
    assert(errors.some((error) => error.includes('npm ci')))
  })
})
