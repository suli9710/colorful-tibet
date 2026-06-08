import { describe, expect, it } from 'vitest'

const nginxConfigPaths = [
  '../../nginx.conf',
  '../../nginx.http.conf'
] as const
const nginxSources = import.meta.glob('../../nginx*.conf', {
  eager: true,
  query: '?raw',
  import: 'default'
}) as Record<string, string>

describe('nginx upload cache headers', () => {
  it.each(nginxConfigPaths)('%s keeps uploaded user media private and revalidated', configPath => {
    const source = nginxSources[configPath]
    expect(source).toBeTypeOf('string')
    const uploadBlock = extractLocationBlock(source, 'location ^~ /uploads/')

    expect(uploadBlock).toContain('expires off;')
    expect(uploadBlock).toContain('proxy_hide_header Cache-Control;')
    expect(uploadBlock).toContain('proxy_hide_header Expires;')
    expect(uploadBlock).toContain('add_header Cache-Control "private, no-cache, must-revalidate" always;')
    expect(uploadBlock).not.toContain('expires 7d;')
  })
})

function extractLocationBlock(source: string, locationPrefix: string) {
  const start = source.indexOf(locationPrefix)
  expect(start).toBeGreaterThanOrEqual(0)

  const openBrace = source.indexOf('{', start)
  expect(openBrace).toBeGreaterThanOrEqual(0)

  let depth = 0
  for (let index = openBrace; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(start, index + 1)
      }
    }
  }

  throw new Error(`Could not find complete block for ${locationPrefix}`)
}
