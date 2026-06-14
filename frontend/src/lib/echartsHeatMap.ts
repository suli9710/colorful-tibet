import { init, registerMap, use } from 'echarts/core'
import { EffectScatterChart, ScatterChart } from 'echarts/charts'
import { GeoComponent, GridComponent, TitleComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

let registered = false

export const ensureHeatMapECharts = () => {
  if (!registered) {
    use([
      EffectScatterChart,
      ScatterChart,
      GeoComponent,
      GridComponent,
      TitleComponent,
      TooltipComponent,
      CanvasRenderer
    ])
    registered = true
  }

  return {
    init,
    registerMap
  }
}

export type { ECharts } from 'echarts/core'
export type { EChartsOption } from 'echarts'
