<template>
  <div class="select-line-container">
    <div class="select-line-card">
      <div class="header">
        <h2 class="title">{{ appTitle }}</h2>
        <p class="subtitle">请选择要连接的线路</p>
      </div>

      <div v-if="loading" class="loading-wrap">
        <i class="el-icon-loading"></i>
        <span>正在加载线路...</span>
      </div>

      <div v-else-if="lines.length === 0" class="empty-wrap">
        <i class="el-icon-warning-outline"></i>
        <p>暂无可用线路，请联系管理员</p>
      </div>

      <div v-else class="line-list">
        <div
          v-for="line in lines"
          :key="line.appId"
          class="line-item"
          @click="selectLine(line)"
        >
          <div class="line-icon">
            <i class="el-icon-connection"></i>
          </div>
          <div class="line-info">
            <div class="line-name">{{ line.appName }}</div>
            <div class="line-detail">{{ line.host }}:{{ line.srvPort }}</div>
          </div>
          <div class="line-arrow">
            <i class="el-icon-arrow-right"></i>
          </div>
        </div>
      </div>

      <div class="footer">
        <el-button type="text" @click="handleLogout">退出登录</el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { getAuthorizedLines } from '@/api/line'
import { removeToken } from '@/utils/auth'

export default {
  name: 'SelectLine',
  data() {
    return {
      loading: true,
      lines: [],
      appTitle: process.env.VUE_APP_TITLE || 'Genlot VPN'
    }
  },
  created() {
    this.loadLines()
  },
  methods: {
    loadLines() {
      this.loading = true
      getAuthorizedLines().then(res => {
        const data = res.data || []
        this.lines = data
      }).catch(() => {
        this.lines = []
      }).finally(() => {
        this.loading = false
      })
    },
    selectLine(line) {
      this.$store.dispatch('SelectLine', line).then(() => {
        this.$router.push('/')
      })
    },
    handleLogout() {
      removeToken()
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
.select-line-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}

.select-line-card {
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  width: 460px;
  max-width: 90vw;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.header {
  text-align: center;
  margin-bottom: 32px;
}

.title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.loading-wrap,
.empty-wrap {
  text-align: center;
  padding: 40px 0;
  color: #909399;
  font-size: 15px;
}

.loading-wrap i,
.empty-wrap i {
  font-size: 36px;
  display: block;
  margin-bottom: 12px;
  color: #c0c4cc;
}

.line-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.line-item {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.line-item:hover {
  border-color: #409eff;
  background: #ecf5ff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.15);
}

.line-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #409eff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  flex-shrink: 0;
}

.line-icon i {
  font-size: 20px;
  color: #fff;
}

.line-info {
  flex: 1;
  min-width: 0;
}

.line-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.line-detail {
  font-size: 13px;
  color: #909399;
}

.line-arrow {
  color: #c0c4cc;
  font-size: 16px;
  margin-left: 12px;
}

.footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}
</style>
