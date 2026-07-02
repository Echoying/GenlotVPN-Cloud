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
    chartData: { type: Object, default: () => ({}) }
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
      const data = this.chartData || {}
      const dates = data.dates || []
      const connectSuccess = data.connectSuccess || []
      const loginFail = data.loginFail || []
      const empty = !dates.length
      this.chart.setOption({
        title: empty ? { text: '暂无数据', left: 'center', top: 'middle', textStyle: { color: '#999', fontSize: 14 } } : undefined,
        tooltip: { trigger: 'axis' },
        legend: { data: ['连接成功', '登录失败'] },
        grid: { left: 16, right: 16, bottom: 24, top: 48, containLabel: true },
        xAxis: { type: 'category', boundaryGap: false, data: dates },
        yAxis: { type: 'value', minInterval: 1 },
        series: [
          {
            name: '连接成功',
            type: 'line',
            smooth: true,
            data: connectSuccess,
            itemStyle: { color: '#67C23A' }
          },
          {
            name: '登录失败',
            type: 'line',
            smooth: true,
            data: loginFail,
            itemStyle: { color: '#F56C6C' }
          }
        ]
      }, true)
    }
  }
}
</script>
