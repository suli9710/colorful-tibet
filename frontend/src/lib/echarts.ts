import { graphic, init, registerMap, use } from 'echarts/core'
import { BarChart, EffectScatterChart, LineChart, PieChart, ScatterChart } from 'echarts/charts'
import { GeoComponent, GridComponent, LegendComponent, TitleComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

let registered = false

export const ensureECharts = () => {
  if (!registered) {
    use([
      BarChart,
      EffectScatterChart,
      LineChart,
      PieChart,
      ScatterChart,
      GeoComponent,
      GridComponent,
      LegendComponent,
      TitleComponent,
      TooltipComponent,
      CanvasRenderer
    ])
    registered = true
  }

  return {
    graphic,
    init,
    registerMap
  }
}

export type { ECharts } from 'echarts/core'
