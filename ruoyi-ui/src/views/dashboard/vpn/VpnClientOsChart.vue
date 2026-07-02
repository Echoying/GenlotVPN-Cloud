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
      const list = this.chartData || []
      const names = list.map(item => item.name)
      const values = list.map(item => Number(item.value || 0))
      const empty = !names.length
      this.chart.setOption({
        title: empty ? { text: '暂无数据', left: 'center', top: 'middle', textStyle: { color: '#999', fontSize: 14 } } : undefined,
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 16, right: 16, bottom: 24, top: 24, containLabel: true },
        xAxis: { type: 'category', data: names, axisLabel: { interval: 0, rotate: names.length > 4 ? 20 : 0 } },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{
          name: '登录次数',
          type: 'bar',
          barMaxWidth: 40,
          data: values,
          itemStyle: { color: '#409EFF' }
        }]
      }, true)
    }
  }
}
</script>
