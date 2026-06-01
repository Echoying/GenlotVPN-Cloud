<template>
  <div class="app-list-container">
    <div class="app-list-card">
      <!-- 顶部用户信息 -->
      <div class="header">
        <div class="user-info">
          <i class="el-icon-user-solid"></i>
          <span class="username">{{ username }}</span>
        </div>
        <el-button type="text" class="logout-btn" @click="handleLogout">退出登录</el-button>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-wrap">
        <i class="el-icon-loading"></i>
        <span>正在加载应用列表...</span>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!appData || !appData.children || appData.children.length === 0" class="empty-wrap">
        <i class="el-icon-warning-outline"></i>
        <p>当前没有可用的应用，请联系管理员开通权限</p>
      </div>

      <!-- 应用列表 -->
      <div v-else class="app-groups">
        <div v-for="group in appData.children" :key="group.id" class="app-group">
          <div class="group-header" @click="toggleGroup(group.id)">
            <i :class="expandedGroups[group.id] !== false ? 'el-icon-arrow-down' : 'el-icon-arrow-right'"></i>
            <span class="group-name">{{ group.name }}</span>
            <span class="group-count">{{ getServiceCount(group) }} 个应用</span>
          </div>
          <div v-show="expandedGroups[group.id] !== false" class="group-services">
            <div
              v-for="service in getVisibleServices(group)"
              :key="service.id"
              class="service-item"
            >
              <span class="service-name">{{ service.name }}</span>
              <span class="service-url">{{ service.url || '-' }}</span>
              <span class="service-type">{{ formatType(service.type) }}</span>
            </div>
            <div v-if="getVisibleServices(group).length === 0" class="no-service">
              暂无应用
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import { getUserInfo, getRedirectUrl, logout } from '@/api/controller'
import { removeToken } from '@/utils/auth'

export default {
  name: 'AppList',
  data() {
    return {
      username: '',
      loading: true,
      appData: null,
      expandedGroups: {}
    }
  },
  created() {
    this.loadUserInfo()
    this.loadAppList()
  },
  methods: {
    loadUserInfo() {
      getUserInfo().then(res => {
        if (res.code === '200' && res.data) {
          this.username = res.data.name || res.data.account || ''
        }
      }).catch(() => {
        this.username = ''
      })
    },
    loadAppList() {
      this.loading = true
      getRedirectUrl().then(res => {
        if (res.code === '200' && res.data) {
          this.appData = res.data
          // 默认展开所有组
          if (res.data.children) {
            res.data.children.forEach(group => {
              this.$set(this.expandedGroups, group.id, true)
            })
          }
        } else {
          this.appData = null
        }
      }).catch(() => {
        this.appData = null
      }).finally(() => {
        this.loading = false
      })
    },
    toggleGroup(groupId) {
      this.$set(this.expandedGroups, groupId, !this.expandedGroups[groupId])
    },
    getVisibleServices(group) {
      if (!group.serviceList) return []
      return group.serviceList.filter(s => s.ifShow !== false)
    },
    getServiceCount(group) {
      return this.getVisibleServices(group).length
    },
    formatType(type) {
      if (type === 'cs') return 'CS'
      if (type === 'bs') return 'BS'
      return type || '-'
    },
    handleLogout() {
      logout().then(res => {
        if (res.code === '200') {
          removeToken()
          this.$router.push('/login')
        }
      }).catch(err => {
        // HTTP状态码非200时，尝试从响应体判断是否实际登出成功
        if (err.response && err.response.data && err.response.data.code === '200') {
          removeToken()
          this.$router.push('/login')
        }
      })
    }
  }
}
</script>
<style scoped>
.app-list-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}

.app-list-card {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  width: 700px;
  max-width: 90vw;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  color: #303133;
  font-weight: 600;
}

.user-info i {
  font-size: 20px;
  color: #409eff;
}

.logout-btn {
  color: #909399;
}

.loading-wrap, .empty-wrap {
  text-align: center;
  padding: 60px 0;
  color: #909399;
}

.loading-wrap i, .empty-wrap i {
  font-size: 40px;
  display: block;
  margin-bottom: 16px;
  color: #c0c4cc;
}

.empty-wrap p {
  margin: 0;
  font-size: 14px;
}

.app-group {
  margin-bottom: 16px;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 6px;
  cursor: pointer;
  user-select: none;
}

.group-header:hover {
  background: #ecf5ff;
}

.group-header i {
  font-size: 12px;
  color: #909399;
  transition: transform 0.2s;
}

.group-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  flex: 1;
}

.group-count {
  font-size: 12px;
  color: #909399;
}

.group-services {
  padding: 8px 0 0 28px;
}

.service-item {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
}

.service-item:last-child {
  border-bottom: none;
}

.service-name {
  flex: 2;
  color: #303133;
  font-weight: 500;
}

.service-url {
  flex: 3;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.service-type {
  width: 40px;
  text-align: center;
  color: #409eff;
  font-size: 12px;
  font-weight: 600;
}

.no-service {
  padding: 12px 16px;
  color: #c0c4cc;
  font-size: 13px;
}
</style>
