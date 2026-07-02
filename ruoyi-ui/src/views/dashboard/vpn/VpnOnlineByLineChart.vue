<template>
  <div :class="className" :style="{ height: height, width: width }" />
</template>

<script>
import * as echarts from 'echarts'
require('echarts/theme/macarons')
import resize from '../mixins/resize'

export default {
  mixins: [resize],
  props: {
    className: { type: String, default: 'chart' },
    width: { type: String, default: '100%' },
    height: { type: String, default: '360px' },
    chartData: { type: Array, default: () => [] }
  },
  data() {
    return { chart: null }
  },
  watch: {
    chartData: {
      deep: true,
      handler() {
        this.setOptions()
      }
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.chart = echarts.init(this.$el, 'macarons')
      this.setOptions()
    })
  },
  beforeDestroy() {
    if (this.chart) {
      this.chart.dispose()
      this.chart = null
    }
  },
  methods: {
    setOptions() {
      if (!this.chart) return
      const seriesData = (this.chartData || []).map(item => ({
        name: item.name,
        value: Number(item.value || 0)
      }))
      const empty = !seriesData.length
      this.chart.setOption({
        title: empty ? { text: '暂无在线用户', left: 'center', top: 'middle', textStyle: { color: '#999', fontSize: 14 } } : undefined,
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 0, type: 'scroll' },
        series: [{
          name: '线路在线',
          type: 'pie',
          radius: ['35%', '62%'],
          center: ['50%', '45%'],
          data: seriesData,
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.2)'
            }
          }
        }]
      }, true)
    }
  }
}
</script>
