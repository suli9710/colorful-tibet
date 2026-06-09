import { existsSync, readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptPath = fileURLToPath(import.meta.url)
const repoRoot = path.resolve(path.dirname(scriptPath), '..')
const frontendPackage = path.join(repoRoot, 'frontend', 'package.json')
const workflowPath = path.join(repoRoot, '.github', 'workflows', 'ci.yml')

const requireFromFrontend = createRequire(frontendPackage)

const loadYaml = () => {
  if (!existsSync(path.join(repoRoot, 'frontend', 'node_modules', 'yaml'))) {
    throw new Error('Missing frontend/node_modules/yaml; run npm --prefix frontend ci before workflow validation.')
  }
  return requireFromFrontend('yaml')
}

const addError = (errors, message) => {
  errors.push(message)
}

const getObject = (value) => (value && typeof value === 'object' && !Array.isArray(value) ? value : null)

export const validateWorkflowDefinition = (workflow) => {
  const errors = []
  const root = getObject(workflow)
  if (!root) {
    return ['Workflow YAML must parse to an object.']
  }

  if (root.name !== 'CI') {
    addError(errors, 'Workflow name must remain CI.')
  }

  const permissions = getObject(root.permissions)
  if (!permissions || permissions.contents !== 'read') {
    addError(errors, 'Workflow permissions must keep contents: read at top level.')
  }

  const triggers = getObject(root.on)
  if (!triggers?.push || !Object.hasOwn(triggers, 'pull_request')) {
    addError(errors, 'Workflow must run on push and pull_request.')
  }

  const jobs = getObject(root.jobs)
  if (!jobs) {
    return [...errors, 'Workflow must define jobs.']
  }

  const requiredJobs = [
    'repository-noise',
    'workflow-lint',
    'supply-chain-pin-gate',
    'secret-scan',
    'deployment-config',
    'backend',
    'scrapler',
    'frontend',
    'docker-images'
  ]
  for (const job of requiredJobs) {
    if (!jobs[job]) {
      addError(errors, `Workflow must define required job: ${job}`)
    }
  }

  const workflowLintSteps = jobs['workflow-lint']?.steps ?? []
  const workflowLintText = JSON.stringify(workflowLintSteps)
  if (!workflowLintText.includes('scripts/validate-workflow-yaml.mjs')) {
    addError(errors, 'workflow-lint job must run scripts/validate-workflow-yaml.mjs.')
  }
  if (!workflowLintText.includes('npm --prefix frontend ci')) {
    addError(errors, 'workflow-lint job must install the locked frontend toolchain with npm ci.')
  }

  const gateText = JSON.stringify(jobs['supply-chain-pin-gate']?.steps ?? [])
  if (!gateText.includes('scripts/check-supply-chain-pins.mjs')) {
    addError(errors, 'supply-chain-pin-gate job must run scripts/check-supply-chain-pins.mjs.')
  }

  return errors
}

const main = () => {
  const YAML = loadYaml()
  const source = readFileSync(workflowPath, 'utf8')
  const document = YAML.parseDocument(source, { prettyErrors: true })
  const errors = document.errors.map((error) => error.message)
  errors.push(...validateWorkflowDefinition(document.toJSON()))

  if (errors.length > 0) {
    for (const error of errors) {
      console.error(`::error title=Workflow YAML validation::${error}`)
    }
    process.exitCode = 1
    return
  }

  console.log('Workflow YAML validation passed.')
}

if (process.argv[1] && path.resolve(process.argv[1]) === scriptPath) {
  main()
}
