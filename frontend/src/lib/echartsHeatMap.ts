import { init, registerMap, use } from 'echarts/core'
import { EffectScatterChart, ScatterChart } from 'echarts/charts'
import { GeoComponent, TitleComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

let registered = false

export const ensureHeatMapECharts = () => {
  if (!registered) {
    use([
      EffectScatterChart,
      ScatterChart,
      GeoComponent,
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
