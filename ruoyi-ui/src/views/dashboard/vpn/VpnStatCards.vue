<template>
  <el-row :gutter="20" class="vpn-stat-cards">
    <el-col
      v-for="item in cards"
      :key="item.key"
      :xs="12"
      :sm="8"
      :lg="4"
      class="vpn-stat-cards__col"
    >
      <div class="vpn-stat-card" :class="{ clickable: !!item.route }" @click="handleClick(item)">
        <div class="vpn-stat-card__icon" :class="'icon-' + item.key">
          <svg-icon :icon-class="item.icon" />
        </div>
        <div class="vpn-stat-card__body">
          <div class="vpn-stat-card__title">{{ item.title }}</div>
          <count-to :start-val="0" :end-val="item.value" :duration="1200" class="vpn-stat-card__num" />
          <div v-if="item.subtitle" class="vpn-stat-card__sub">{{ item.subtitle }}</div>
        </div>
      </div>
    </el-col>
  </el-row>
</template>

<script>
import CountTo from 'vue-count-to'

export default {
  name: 'VpnStatCards',
  components: { CountTo },
  props: {
    overview: {
      type: Object,
      default: () => ({})
    }
  },
  computed: {
    cards() {
      const o = this.overview || {}
      return [
        {
          key: 'online',
          title: '当前在线',
          value: Number(o.onlineTotal || 0),
          icon: 'online',
          route: '/yianlian/online'
        },
        {
          key: 'connect',
          title: '今日连接成功',
          value: Number(o.todayConnectSuccess || 0),
          icon: 'link',
          subtitle: this.formatCompare(o.todayConnectSuccess, o.yesterdayConnectSuccess),
          route: '/yianlian/logininfor',
          query: this.todayConnectQuery()
        },
        {
          key: 'fail',
          title: '今日登录失败',
          value: Number(o.todayLoginFail || 0),
          icon: 'bug',
          subtitle: this.formatCompare(o.todayLoginFail, o.yesterdayLoginFail, true),
          route: '/yianlian/logininfor',
          query: this.todayFailQuery()
        },
        {
          key: 'localUser',
          title: '本地用户',
          value: Number(o.localUserTotal || 0),
          icon: 'user',
          route: '/yianlian/local/user'
        },
        {
          key: 'line',
          title: '启用线路',
          value: Number(o.lineEnabledTotal || 0),
          icon: 'client',
          route: '/yianlian/line'
        },
        {
          key: 'authOnly',
          title: '已登录未连接',
          value: Number(o.authenticatedNotConnected || 0),
          icon: 'peoples',
          route: null
        }
      ]
    }
  },
  methods: {
    formatCompare(today, yesterday, reverse) {
      const cur = Number(today || 0)
      const prev = Number(yesterday || 0)
      if (prev === 0) {
        return cur > 0 ? '较昨日新增' : '较昨日持平'
      }
      const pct = Math.round(((cur - prev) / prev) * 100)
      if (pct === 0) {
        return '较昨日持平'
      }
      const up = pct > 0
      const arrow = up ? '↑' : '↓'
      return `较昨日 ${arrow}${Math.abs(pct)}%`
    },
    todayRange() {
      const now = new Date()
      const y = now.getFullYear()
      const m = String(now.getMonth() + 1).padStart(2, '0')
      const d = String(now.getDate()).padStart(2, '0')
      return [`${y}-${m}-${d} 00:00:00`, `${y}-${m}-${d} 23:59:59`]
    },
    todayConnectQuery() {
      const range = this.todayRange()
      return { status: '0', beginTime: range[0], endTime: range[1], msg: '[connect]' }
    },
    todayFailQuery() {
      const range = this.todayRange()
      return { status: '1', beginTime: range[0], endTime: range[1] }
    },
    handleClick(item) {
      if (!item.route) {
        return
      }
      this.$router.push({
        path: item.route,
        query: item.query || {}
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.vpn-stat-cards {
  margin-bottom: 16px;

  &__col {
    margin-bottom: 16px;
  }
}

.vpn-stat-card {
  display: flex;
  align-items: center;
  height: 108px;
  padding: 16px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

  &.clickable {
    cursor: pointer;
    transition: box-shadow 0.2s ease;

    &:hover {
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
    }
  }

  &__icon {
    width: 56px;
    height: 56px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
    margin-right: 12px;
    color: #409eff;
    background: #ecf5ff;
  }

  &__title {
    color: rgba(0, 0, 0, 0.45);
    font-size: 14px;
    margin-bottom: 6px;
  }

  &__num {
    font-size: 24px;
    font-weight: 600;
    color: #303133;
    line-height: 1.2;
  }

  &__sub {
    margin-top: 4px;
    font-size: 12px;
    color: #909399;
  }
}
</style>
