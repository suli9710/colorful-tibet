// @vitest-environment node
import { describe, expect, it } from 'vitest'
import { createRequire } from 'node:module'
import { dirname, join } from 'node:path'
import openapiTS, { astToString } from 'openapi-typescript'
import { init, use } from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, LegendComponent } from 'echarts/components'
import { SVGRenderer } from 'echarts/renderers'

describe('security dependency upgrade compatibility', () => {
  it('keeps OpenAPI 7 generation and local reference resolution operational', async () => {
    const output = astToString(await openapiTS({
      openapi: '3.0.3', info: { title: 'Compatibility', version: '1' },
      paths: { '/health': { get: { responses: { '200': {
        description: 'OK', content: { 'application/json': { schema: { $ref: '#/components/schemas/Health' } } }
      } } } } },
      components: { schemas: { Health: { type: 'object', required: ['ok'], properties: { ok: { type: 'boolean' } } } } }
    }))
    expect(output).toContain('ok: boolean')
    expect(output).toContain('/health')
  })

  it('preserves the named minimatch API consumed by TypeScript ESTree', () => {
    // This override is intentionally scoped; Redocly 1 still needs the older callable API.
    const localRequire = createRequire(import.meta.resolve('@typescript-eslint/typescript-estree'))
    const { minimatch } = localRequire('minimatch')
    expect(minimatch('src/i18n/index.ts', 'src/**/*.ts', { dot: true })).toBe(true)
    expect(minimatch('src/i18n/index.js', 'src/**/*.ts', { dot: true })).toBe(false)
  })

  it('matches positive and negative URL globs through the patched Redocly API', async () => {
    const require = createRequire(import.meta.url)
    const openapiRequire = createRequire(require.resolve('openapi-typescript'))
    const root = dirname(openapiRequire.resolve('@redocly/openapi-core'))
    const { readFileFromUrl } = openapiRequire(join(root, 'utils.js'))
    let received: Record<string, string> = {}
    await readFileFromUrl('https://api.example.test/schema.json', {
      headers: [
        { matches: 'api.example.test/**', name: 'X-Matching', value: 'yes' },
        { matches: 'https://other.example.test/**', name: 'X-Nonmatching', value: 'no' }
      ],
      customFetch: async (_url: string, options: { headers: Record<string, string> }) => {
        received = options.headers
        return new Response('{}', { headers: { 'content-type': 'application/json' } })
      }
    })
    expect(received).toEqual({ 'X-Matching': 'yes' })
  })

  it('renders registered ECharts 6 modules to SVG without a browser', () => {
    use([BarChart, GridComponent, LegendComponent, SVGRenderer])
    const chart = init(null, null, { renderer: 'svg', ssr: true, width: 700, height: 400 })
    try {
      chart.setOption({ animation: false, legend: { top: 6 }, xAxis: { type: 'category', data: ['A', 'B'] }, yAxis: { type: 'value' }, series: [{ name: 'Bookings', type: 'bar', data: [2, 3] }] })
      const svg = chart.renderToSVGString()
      expect(svg).toContain('<svg')
      expect(svg).toContain('Bookings')
    } finally { chart.dispose() }
  })
})
