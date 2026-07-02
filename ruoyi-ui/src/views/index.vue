<template>
  <div class="app-container vpn-dashboard">
    <div class="vpn-dashboard__header">
      <div>
        <h2 class="vpn-dashboard__title">Genlot VPN 管理</h2>
        <p class="vpn-dashboard__version">当前版本: v{{ version }}</p>
      </div>
      <div v-if="hasDashboardPerm" class="vpn-dashboard__actions">
        <span v-if="lastRefreshText" class="vpn-dashboard__refresh-text">最后刷新: {{ lastRefreshText }}</span>
        <el-button size="mini" icon="el-icon-refresh" :loading="refreshing" @click="refreshAll">刷新</el-button>
      </div>
    </div>

    <template v-if="hasDashboardPerm">
      <div v-loading="overviewLoading">
        <vpn-stat-cards :overview="overview" />
      </div>

      <el-row :gutter="20" class="vpn-dashboard__charts">
        <el-col :xs="24" :lg="14">
          <div class="vpn-dashboard__panel" v-loading="trendLoading">
            <div class="vpn-dashboard__panel-header">
              <span>连接趋势</span>
              <el-radio-group v-model="trendDays" size="mini" @change="loadTrend">
                <el-radio-button :label="7">近7天</el-radio-button>
                <el-radio-button :label="30">近30天</el-radio-button>
              </el-radio-group>
            </div>
            <vpn-login-trend-chart :chart-data="trendData" />
          </div>
        </el-col>
        <el-col :xs="24" :lg="10">
          <div class="vpn-dashboard__panel" v-loading="distLoading">
            <div class="vpn-dashboard__panel-header">
              <span>各线路当前在线</span>
            </div>
            <vpn-online-by-line-chart :chart-data="distributions.onlineByLine || []" />
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="vpn-dashboard__charts">
        <el-col :xs="24" :lg="12">
          <div class="vpn-dashboard__panel" v-loading="distLoading">
            <div class="vpn-dashboard__panel-header">
              <span>客户端 OS 分布（近7日）</span>
            </div>
            <vpn-client-os-chart :chart-data="distributions.clientOs || []" />
          </div>
        </el-col>
        <el-col :xs="24" :lg="12">
          <div class="vpn-dashboard__panel" v-loading="distLoading">
            <div class="vpn-dashboard__panel-header">
              <span>登录失败原因 Top5（近7日）</span>
            </div>
            <vpn-fail-reason-chart :chart-data="distributions.failReasons || []" />
          </div>
        </el-col>
      </el-row>

      <vpn-recent-events :loading="eventsLoading" :events="recentEvents" />
    </template>

    <el-empty v-else description="暂无 VPN 仪表盘查看权限" />
  </div>
</template>

<script>
import auth from '@/plugins/auth'
import { getOverview, getLoginTrend, getDistributions, getRecentEvents } from '@/api/vpn/dashboard'
import VpnStatCards from './dashboard/vpn/VpnStatCards'
import VpnLoginTrendChart from './dashboard/vpn/VpnLoginTrendChart'
import VpnOnlineByLineChart from './dashboard/vpn/VpnOnlineByLineChart'
import VpnClientOsChart from './dashboard/vpn/VpnClientOsChart'
import VpnFailReasonChart from './dashboard/vpn/VpnFailReasonChart'
import VpnRecentEvents from './dashboard/vpn/VpnRecentEvents'

export default {
  name: 'Index',
  components: {
    VpnStatCards,
    VpnLoginTrendChart,
    VpnOnlineByLineChart,
    VpnClientOsChart,
    VpnFailReasonChart,
    VpnRecentEvents
  },
  data() {
    return {
      version: '3.6.8',
      overview: {},
      trendData: {},
      distributions: {},
      recentEvents: [],
      trendDays: 7,
      distDays: 7,
      overviewLoading: false,
      trendLoading: false,
      distLoading: false,
      eventsLoading: false,
      refreshing: false,
      lastRefreshText: '',
      autoRefreshTimer: null
    }
  },
  computed: {
    hasDashboardPerm() {
      return auth.hasPermi('vpn:dashboard:view')
    }
  },
  created() {
    if (this.hasDashboardPerm) {
      this.refreshAll()
      this.startAutoRefresh()
    }
  },
  beforeDestroy() {
    this.stopAutoRefresh()
  },
  methods: {
    refreshAll() {
      this.refreshing = true
      Promise.all([
        this.loadOverview(),
        this.loadTrend(),
        this.loadDistributions(),
        this.loadRecentEvents()
      ]).finally(() => {
        this.refreshing = false
        this.lastRefreshText = this.parseTime(new Date())
      })
    },
    loadOverview() {
      this.overviewLoading = true
      return getOverview().then(res => {
        this.overview = res.data || {}
      }).finally(() => {
        this.overviewLoading = false
      })
    },
    loadTrend() {
      this.trendLoading = true
      return getLoginTrend(this.trendDays).then(res => {
        this.trendData = res.data || {}
      }).finally(() => {
        this.trendLoading = false
      })
    },
    loadDistributions() {
      this.distLoading = true
      return getDistributions(this.distDays).then(res => {
        this.distributions = res.data || {}
      }).finally(() => {
        this.distLoading = false
      })
    },
    loadRecentEvents() {
      this.eventsLoading = true
      return getRecentEvents(10).then(res => {
        this.recentEvents = res.data || []
      }).finally(() => {
        this.eventsLoading = false
      })
    },
    refreshOnlineMetrics() {
      return Promise.all([this.loadOverview(), this.loadDistributions()])
    },
    startAutoRefresh() {
      this.stopAutoRefresh()
      this.autoRefreshTimer = setInterval(() => {
        this.refreshOnlineMetrics()
      }, 60000)
    },
    stopAutoRefresh() {
      if (this.autoRefreshTimer) {
        clearInterval(this.autoRefreshTimer)
        this.autoRefreshTimer = null
      }
    }
  }
}
</script>

<style scoped lang="scss">
.vpn-dashboard {
  &__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 8px;
  }

  &__title {
    margin: 0;
    font-size: 26px;
    font-weight: 500;
    color: #303133;
  }

  &__version {
    margin: 8px 0 0;
    color: #909399;
    font-size: 13px;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__refresh-text {
    color: #909399;
    font-size: 12px;
  }

  &__charts {
    margin-bottom: 8px;
  }

  &__panel {
    background: #fff;
    padding: 16px 16px 0;
    margin-bottom: 16px;
    border-radius: 4px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  }

  &__panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }
}
</style>
