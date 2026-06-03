<template>
  <div class="app-list-container">
    <div class="app-list-card">
      <!-- 用户信息 -->
      <div class="header">
        <div class="user-info">
          <i class="el-icon-user-solid"></i>
          <span class="username">{{ username }}</span>
        </div>
        <div class="line-info" :title="lineDisplayName">
          <i class="el-icon-connection"></i>
          <span class="line-name">{{ lineDisplayName }}</span>
        </div>
        <el-button type="text" class="logout-btn" @click="handleLogout">退出登录</el-button>
      </div>

      <!-- 网关连接 -->
      <div class="gateway-section">
        <div class="gateway-title">网关连接</div>
        <div v-if="gatewayLoading" class="gateway-loading">
          <i class="el-icon-loading"></i>
          <span>正在获取网关…</span>
        </div>
        <div v-else-if="gatewayList.length === 0" class="gateway-empty">
          <i class="el-icon-warning-outline"></i>
          <span>暂无可用网关</span>
        </div>
        <div v-else class="gateway-bar">
          <div
            v-for="gw in gatewayList"
            :key="gw.id"
            class="gateway-item"
            :class="{
              active: selectedGatewayId === gw.id,
              switching: switchingGatewayId === gw.id
            }"
            @click="handleGatewaySelect(gw)"
          >
            <div class="gateway-item-header">
              <span class="gateway-name">{{ gw.name }}</span>
              <i v-if="switchingGatewayId === gw.id" class="el-icon-loading"></i>
            </div>
            <div class="gateway-item-tags">
              <el-tag
                v-if="gw.id === selectedGatewayId && gw.tunnelStatusLabel"
                :class="['tunnel-status-tag', gw.tunnelStatusClass]"
                size="mini"
                effect="dark"
              >{{ gw.tunnelStatusLabel }}</el-tag>
              <el-tag v-else-if="gw.connected" type="success" size="mini" effect="plain">已连接</el-tag>
              <el-tag v-else type="info" size="mini" effect="plain">未连接</el-tag>
            </div>
            <div v-if="gw.delayTime != null && gw.delayTime !== ''" class="gateway-delay">
              延时 {{ gw.delayTime }}
            </div>
          </div>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-wrap">
        <i class="el-icon-loading"></i>
        <span>正在加载应用列表...</span>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!hasVisibleApps" class="empty-wrap">
        <i class="el-icon-warning-outline"></i>
        <p>当前没有可用的应用，请联系管理员开通权限</p>
      </div>

      <!-- 应用列表 -->
      <div v-else class="app-groups">
        <div v-for="group in visibleGroups" :key="group.id" class="app-group">
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
              <div class="service-name-col">
                <span class="service-name">{{ service.name }}</span>
                <el-tag v-if="service.clickStatus === false" type="warning" size="mini">不可用</el-tag>
                <el-tag v-if="service.connectState === '1'" type="info" size="mini">已禁用</el-tag>
              </div>
              <div class="service-url-wrap">
                <span class="service-url">{{ service.url || '-' }}</span>
                <el-button
                  v-if="isHttpUrl(service.url)"
                  type="text"
                  icon="el-icon-document-copy"
                  class="copy-url-btn"
                  title="复制链接"
                  @click.stop="copyServiceUrl(service.url)"
                />
              </div>
              <span class="service-type">{{ formatType(service.type) }}</span>
              <span v-if="service.reason" class="service-reason" :title="service.reason">{{ service.reason }}</span>
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
import {
  getUserInfo,
  getUserGroupedServiceList,
  logout,
  getGatewayList,
  getTunnelStatus,
  turnOnGateway,
  switchGateway
} from '@/api/controller'

const TUNNEL_STATUS_POLL_INTERVAL = 10000
import { mapState } from 'vuex'

export default {
  name: 'AppList',
  data() {
    return {
      username: '',
      loading: true,
      appData: null,
      expandedGroups: {},
      gatewayLoading: true,
      gatewayList: [],
      selectedGatewayId: null,
      switchingGatewayId: null,
      tunnelStatus: null,
      tunnelReConnect: false,
      tunnelStatusTimer: null
    }
  },
  computed: {
    ...mapState({
      selectedLine: state => state.user.selectedLine
    }),
    lineDisplayName() {
      if (!this.selectedLine) {
        return '当前线路'
      }
      if (this.selectedLine.appName) {
        return this.selectedLine.appName
      }
      if (this.selectedLine.host) {
        const port = this.selectedLine.srvPort ? `:${this.selectedLine.srvPort}` : ''
        return `${this.selectedLine.host}${port}`
      }
      return '当前线路'
    },
    visibleGroups() {
      if (!this.appData || !this.appData.children) return []
      return this.appData.children.filter(group => this.getVisibleServices(group).length > 0)
    },
    hasVisibleApps() {
      return this.visibleGroups.length > 0
    }
  },
  created() {
    this.loadUserInfo()
    this.loadAppList()
    this.initGatewayFlow()
  },
  beforeDestroy() {
    this.stopTunnelStatusPolling()
  },
  methods: {
    mapTunnelStatus(status) {
      const s = Number(status)
      switch (s) {
        case 1:
          return { connected: false, statusLabel: '连接中', statusClass: 'tunnel-status-1' }
        case 2:
          return { connected: true, statusLabel: '已连接', statusClass: 'tunnel-status-2' }
        case 3:
          return { connected: false, statusLabel: '断开连接中', statusClass: 'tunnel-status-3' }
        case 4:
          return { connected: false, statusLabel: '已断开', statusClass: 'tunnel-status-4' }
        case 0:
        default:
          return { connected: false, statusLabel: '未连接', statusClass: 'tunnel-status-0' }
      }
    },
    applyTunnelStatusToSelectedGateway() {
      if (this.tunnelStatus == null || !this.selectedGatewayId) return
      const gw = this.gatewayList.find(g => g.id === this.selectedGatewayId)
      if (!gw) return
      const mapped = this.mapTunnelStatus(this.tunnelStatus)
      this.$set(gw, 'connected', mapped.connected)
      this.$set(gw, 'tunnelStatusLabel', mapped.statusLabel)
      this.$set(gw, 'tunnelStatusClass', mapped.statusClass)
    },
    clearTunnelStatusOnGateway(gw) {
      if (!gw) return
      this.$delete(gw, 'tunnelStatusLabel')
      this.$delete(gw, 'tunnelStatusClass')
    },
    async refreshGatewayAndTunnelStatus() {
      if (this.gatewayList.length === 0) return
      try {
        const [gwRes, tunnelRes] = await Promise.all([
          getGatewayList(),
          getTunnelStatus()
        ])
        if (gwRes.code === '200' && gwRes.data && Array.isArray(gwRes.data.list)) {
          const prevId = this.selectedGatewayId
          this.gatewayList = gwRes.data.list.map(item => ({ ...item }))
          if (prevId && this.gatewayList.some(g => g.id === prevId)) {
            this.selectedGatewayId = prevId
          } else if (this.gatewayList.length > 0) {
            this.selectedGatewayId = this.gatewayList[0].id
          }
          this.gatewayList.forEach(gw => this.clearTunnelStatusOnGateway(gw))
        }
        if (tunnelRes.code === '200' && tunnelRes.data) {
          this.tunnelStatus = tunnelRes.data.status
          this.tunnelReConnect = !!tunnelRes.data.reConnect
          this.applyTunnelStatusToSelectedGateway()
        }
      } catch (e) {
        // 轮询失败静默，避免每 10 秒弹窗
      }
    },
    startTunnelStatusPolling() {
      this.stopTunnelStatusPolling()
      this.refreshGatewayAndTunnelStatus()
      this.tunnelStatusTimer = setInterval(() => {
        this.refreshGatewayAndTunnelStatus()
      }, TUNNEL_STATUS_POLL_INTERVAL)
    },
    stopTunnelStatusPolling() {
      if (this.tunnelStatusTimer) {
        clearInterval(this.tunnelStatusTimer)
        this.tunnelStatusTimer = null
      }
    },
    groupFlatServiceList(flatList) {
      if (!Array.isArray(flatList) || flatList.length === 0) {
        return { children: [] }
      }
      const groupMap = new Map()
      flatList.forEach(item => {
        const gid = item.groupId || 'default'
        if (!groupMap.has(gid)) {
          groupMap.set(gid, {
            id: gid,
            name: item.groupName || '未分组',
            groupSort: item.groupSort,
            groupSortNumber: item.groupSortNumber,
            serviceList: []
          })
        }
        groupMap.get(gid).serviceList.push({
          id: item.id || item.serviceId,
          name: item.name || item.serviceName,
          type: item.type || item.serviceType,
          url: item.url || item.urlPlus || '',
          ifShow: item.ifShow,
          clickStatus: item.clickStatus,
          connectState: item.connectState,
          reason: item.reason,
          status: item.status,
          serviceSort: item.serviceSort
        })
      })
      const children = Array.from(groupMap.values())
      children.forEach(group => {
        group.serviceList.sort((a, b) => (a.serviceSort ?? 0) - (b.serviceSort ?? 0))
      })
      children.sort((a, b) => {
        const sortA = a.groupSort ?? 0
        const sortB = b.groupSort ?? 0
        if (sortA !== sortB) return sortA - sortB
        return String(a.groupSortNumber || '').localeCompare(String(b.groupSortNumber || ''))
      })
      return { children }
    },
    async initGatewayFlow() {
      this.gatewayLoading = true
      this.gatewayList = []
      try {
        const res = await getGatewayList()
        if (res.code === '200' && res.data && Array.isArray(res.data.list)) {
          this.gatewayList = res.data.list
          if (this.gatewayList.length > 0) {
            this.selectedGatewayId = this.gatewayList[0].id
            try {
              await turnOnGateway(true)
            } catch (err) {
              this.$alert(err.message || '打开网关连接失败', '打开网关失败', { type: 'error' })
            }
          }
        }
      } catch (err) {
        this.gatewayList = []
        this.$alert(err.message || '获取网关列表失败', '提示', { type: 'error' })
      } finally {
        this.gatewayLoading = false
        if (this.gatewayList.length > 0) {
          this.startTunnelStatusPolling()
        }
      }
    },
    async handleGatewaySelect(gw) {
      if (!gw || !gw.id) return
      if (this.switchingGatewayId) return
      if (this.selectedGatewayId === gw.id) return

      this.switchingGatewayId = gw.id
      try {
        await switchGateway(gw.id)
        this.selectedGatewayId = gw.id
        await this.refreshGatewayAndTunnelStatus()
      } catch (err) {
        this.$alert(err.message || '切换网关失败', '提示', { type: 'error' })
      } finally {
        this.switchingGatewayId = null
      }
    },
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
      getUserGroupedServiceList('').then(res => {
        if (res.code === '200' && Array.isArray(res.data)) {
          this.appData = this.groupFlatServiceList(res.data)
          this.expandedGroups = {}
          if (this.appData.children) {
            this.appData.children.forEach(group => {
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
      return group.serviceList.filter(s => {
        if (s.ifShow === false) return false
        const name = (s.name || '').trim()
        return name.length > 0
      })
    },
    isHttpUrl(url) {
      if (!url || typeof url !== 'string') return false
      const u = url.trim().toLowerCase()
      return u.startsWith('http://') || u.startsWith('https://')
    },
    async copyServiceUrl(url) {
      const text = (url || '').trim()
      if (!text) return
      try {
        if (navigator.clipboard && navigator.clipboard.writeText) {
          await navigator.clipboard.writeText(text)
        } else {
          const textarea = document.createElement('textarea')
          textarea.value = text
          textarea.style.position = 'fixed'
          textarea.style.left = '-9999px'
          document.body.appendChild(textarea)
          textarea.select()
          document.execCommand('copy')
          document.body.removeChild(textarea)
        }
        this.$message.success('链接已复制到剪贴板')
      } catch (e) {
        this.$message.error('复制失败，请手动复制')
      }
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
      this.stopTunnelStatusPolling()
      logout().then(res => {
        if (res.code === '200') {
          this.$store.dispatch('LogOut').then(() => {
            this.$router.push('/login')
          })
        }
      }).catch(err => {
        if (err.response && err.response.data && err.response.data.code === '200') {
          this.$store.dispatch('LogOut').then(() => {
            this.$router.push('/login')
          })
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
  width: 800px;
  max-width: 90vw;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.gateway-section {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.gateway-title {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 12px;
}

.gateway-loading,
.gateway-empty {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  color: #909399;
  font-size: 14px;
  background: #f5f7fa;
  border-radius: 8px;
}

.gateway-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.gateway-item {
  flex: 1;
  min-width: 140px;
  max-width: 220px;
  padding: 12px 14px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafafa;
}

.gateway-item:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

.gateway-item.active {
  border-color: #409eff;
  background: #ecf5ff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.gateway-item.switching {
  cursor: wait;
  opacity: 0.85;
}

.gateway-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.gateway-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gateway-item-tags {
  margin-bottom: 4px;
}

/* 隧道连接状态：0未连接 1连接中 2已连接 3断开连接中 4已断开 */
.tunnel-status-tag {
  border: none;
}
.tunnel-status-tag.tunnel-status-0 {
  background-color: #909399;
  color: #fff;
}
.tunnel-status-tag.tunnel-status-1 {
  background-color: #409eff;
  color: #fff;
}
.tunnel-status-tag.tunnel-status-2 {
  background-color: #67c23a;
  color: #fff;
}
.tunnel-status-tag.tunnel-status-3 {
  background-color: #e6a23c;
  color: #fff;
}
.tunnel-status-tag.tunnel-status-4 {
  background-color: #f56c6c;
  color: #fff;
}

.gateway-delay {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.header {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
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
  justify-self: start;
  min-width: 0;
}

.user-info i {
  font-size: 20px;
  color: #409eff;
  flex-shrink: 0;
}

.username {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.line-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  justify-self: center;
  max-width: 100%;
  padding: 0 8px;
  color: #606266;
  font-size: 15px;
  font-weight: 600;
}

.line-info i {
  font-size: 18px;
  color: #409eff;
  flex-shrink: 0;
}

.line-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout-btn {
  color: #909399;
  justify-self: end;
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
  flex-wrap: wrap;
  gap: 6px 8px;
  padding: 10px 16px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
}

.service-item:last-child {
  border-bottom: none;
}

.service-name-col {
  flex: 0 0 140px;
  max-width: 140px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.service-name {
  color: #303133;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.service-reason {
  flex: 1 1 100%;
  font-size: 12px;
  color: #e6a23c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.service-url-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

.service-url {
  flex: 1;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.copy-url-btn {
  flex-shrink: 0;
  padding: 0 4px;
  font-size: 16px;
  color: #409eff;
}

.service-type {
  flex: 0 0 36px;
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
