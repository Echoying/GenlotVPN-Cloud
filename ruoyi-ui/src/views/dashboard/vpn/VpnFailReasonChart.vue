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
    height: { type: String, default: '320px' },
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
      const list = [...(this.chartData || [])].reverse()
      const names = list.map(item => this.truncate(item.name, 28))
      const values = list.map(item => Number(item.value || 0))
      const empty = !names.length
      this.chart.setOption({
        title: empty ? { text: '暂无失败记录', left: 'center', top: 'middle', textStyle: { color: '#999', fontSize: 14 } } : undefined,
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 16, right: 24, bottom: 16, top: 16, containLabel: true },
        xAxis: { type: 'value', minInterval: 1 },
        yAxis: { type: 'category', data: names },
        series: [{
          name: '失败次数',
          type: 'bar',
          data: values,
          itemStyle: { color: '#F56C6C' }
        }]
      }, true)
    },
    truncate(text, max) {
      const val = text || '未知'
      return val.length > max ? val.slice(0, max) + '...' : val
    }
  }
}
</script>
