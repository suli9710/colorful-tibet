import { graphic, init, use } from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

let registered = false

export const ensureAdminECharts = () => {
  if (!registered) {
    use([
      BarChart,
      LineChart,
      PieChart,
      GridComponent,
      LegendComponent,
      TooltipComponent,
      CanvasRenderer
    ])
    registered = true
  }

  return {
    graphic,
    init
  }
}

export type { ECharts } from 'echarts/core'
