import { spawnSync } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const envFilter = `
OLD_EMAIL1=cursoragent@cursor.com
OLD_EMAIL2=2173167484@qq.com
CORRECT_NAME=suli9710
CORRECT_EMAIL=117092449+suli9710@users.noreply.github.com
if [ "$GIT_AUTHOR_EMAIL" = "$OLD_EMAIL1" ] || [ "$GIT_AUTHOR_EMAIL" = "$OLD_EMAIL2" ]; then
  export GIT_AUTHOR_NAME="$CORRECT_NAME"
  export GIT_AUTHOR_EMAIL="$CORRECT_EMAIL"
fi
if [ "$GIT_COMMITTER_EMAIL" = "$OLD_EMAIL1" ] || [ "$GIT_COMMITTER_EMAIL" = "$OLD_EMAIL2" ]; then
  export GIT_COMMITTER_NAME="$CORRECT_NAME"
  export GIT_COMMITTER_EMAIL="$CORRECT_EMAIL"
fi
`.trim()

const result = spawnSync(
  'git',
  ['filter-branch', '-f', '--env-filter', envFilter, '--', '--all'],
  {
    cwd: repoRoot,
    env: { ...process.env, FILTER_BRANCH_SQUELCH_WARNING: '1' },
    stdio: 'inherit',
    shell: false
  }
)

if (result.status !== 0) {
  process.exit(result.status ?? 1)
}

spawnSync('git', ['shortlog', '-sne', '--all'], {
  cwd: repoRoot,
  stdio: 'inherit',
  shell: false
})
