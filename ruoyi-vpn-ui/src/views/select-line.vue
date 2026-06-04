<template>
  <div class="select-line-container">
    <div class="select-line-card">
      <div class="header">
        <h2 class="title">{{ appTitle }}</h2>
        <p class="subtitle">{{ connectOnly ? ('正在连接：' + (activeLineName || '')) : '请选择要连接的线路' }}</p>
      </div>

      <div class="content-wrapper">
        <!-- 左侧线路列表（登录前已选线时隐藏列表，仅展示连接进度） -->
        <div class="line-section" v-if="!connectOnly">
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
              :class="{ 'detecting': detectingLineId === line.appId }"
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
                <i v-if="detectingLineId === line.appId" class="el-icon-loading"></i>
                <i v-else class="el-icon-arrow-right"></i>
              </div>
            </div>
          </div>
        </div>

        <!-- 仅连接模式：占满宽度的日志区 -->
        <div class="log-section" :class="{ 'log-section-full': connectOnly }">
          <div class="log-header">
            <i class="el-icon-document"></i>
            <span>日志信息</span>
          </div>
          <div class="log-content" ref="logContent">
            <div v-if="logs.length === 0" class="log-empty">
              <i class="el-icon-info"></i>
              <p>{{ connectOnly ? '正在进行安全验证与连接...' : '点击线路开始检测' }}</p>
            </div>
            <div v-else class="log-list">
              <div
                v-for="(log, index) in logs"
                :key="index"
                class="log-item"
                :class="log.type"
              >
                <span class="log-time">{{ log.time }}</span>
                <span class="log-message">{{ log.message }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="footer">
        <el-button type="text" @click="handleLogout">退出登录</el-button>
      </div>
    </div>

    <!-- 选线安全验证弹窗 -->
    <el-dialog
      title="选线安全验证"
      :visible.sync="verifyDialogVisible"
      width="480px"
      :close-on-click-modal="false"
      @close="handleVerifyDialogClose"
    >
      <p v-if="pendingLine" class="verify-line-hint">当前线路：{{ pendingLine.appName }}</p>
      <div class="verify-code-row">
        <el-input
          v-model="verifyCode"
          placeholder="请输入钉钉验证码"
          maxlength="6"
          clearable
          class="verify-code-input"
          @keyup.enter.native="handleConfirmVerify"
        />
        <el-button
          type="primary"
          :disabled="sendCountdown > 0 || sendingCode"
          :loading="sendingCode"
          @click="handleSendCode"
        >
          {{ sendCountdown > 0 ? `重新发送(${sendCountdown}s)` : '发送验证码' }}
        </el-button>
        <el-tooltip placement="top" effect="dark">
          <div slot="content" class="verify-tooltip-content">
            钉钉验证码会发送到 VPN 群中，VPN 验证码信息机器人会把验证码信息发送到群里，请把收到的验证码回填。
          </div>
          <i class="el-icon-question verify-help-icon"></i>
        </el-tooltip>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="handleVerifyDialogClose">取 消</el-button>
        <el-button type="primary" :loading="confirmingVerify" @click="handleConfirmVerify">确 认</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getAuthorizedLines, getUserCredentials } from '@/api/line'
import { sendLineVerifyCode, confirmLineVerifyCode } from '@/api/lineVerify'
import { detectServer, selectServer, getServerVersion, getClientVersion, loginWithAccount } from '@/api/controller'
import { removeToken } from '@/utils/auth'
import { getPendingLine, clearPendingLine } from '@/utils/pendingLine'

export default {
  name: 'SelectLine',
  data() {
    return {
      connectOnly: false,
      activeLineName: '',
      loading: true,
      lines: [],
      logs: [],
      detectingLineId: null,
      appTitle: process.env.VUE_APP_TITLE || 'Genlot VPN',
      verifyDialogVisible: false,
      pendingLine: null,
      verifyCode: '',
      sendCountdown: 0,
      countdownTimer: null,
      sendingCode: false,
      confirmingVerify: false
    }
  },
  created() {
    this.connectOnly = this.$route.query.connect === '1'
    this.loadLines()
  },
  beforeDestroy() {
    this.clearSendCountdown()
  },
  methods: {
    loadLines() {
      this.loading = true
      const pending = this.$store.state.user.pendingLine || getPendingLine()
      this.addLog('info', '正在加载授权线路...')
      getAuthorizedLines().then(res => {
        const data = res.data || []
        if (this.connectOnly && pending) {
          const matched = data.find(l => l.appId === pending.appId)
          if (!matched) {
            this.lines = []
            this.addLog('error', '您无权访问所选线路，请重新选择')
            this.$message.error('您无权访问所选线路')
            this.loading = false
            return
          }
          this.lines = [matched]
          this.activeLineName = matched.appName
          this.addLog('info', `当前线路: ${matched.appName}`)
          this.loading = false
          this.$nextTick(() => this.selectLine(matched))
          return
        }
        this.lines = data
        this.addLog('info', `成功加载 ${data.length} 条线路`)
      }).catch(err => {
        this.lines = []
        this.addLog('error', '加载线路失败: ' + (err.message || '未知错误'))
      }).finally(() => {
        if (!this.connectOnly || !pending) {
          this.loading = false
        }
      })
    },
    selectLine(line) {
      if (this.detectingLineId) {
        this.$message.warning('正在检测其他线路，请稍候')
        return
      }
      this.pendingLine = line
      this.verifyCode = ''
      this.verifyDialogVisible = true
    },
    handleSendCode() {
      if (!this.pendingLine || this.sendCountdown > 0) {
        return
      }
      this.sendingCode = true
      sendLineVerifyCode({
        appId: this.pendingLine.appId,
        lineName: this.pendingLine.appName
      }).then(res => {
        this.$message.success('验证码已发送到 VPN 群，请查收')
        if (res.data && res.data.validSeconds) {
          this.addLog('info', `验证码有效时间: ${res.data.validSeconds}秒`)
        }
        this.startSendCountdown(60)
      }).catch(err => {
        this.$message.error(err.message || '发送验证码失败')
      }).finally(() => {
        this.sendingCode = false
      })
    },
    handleConfirmVerify() {
      if (!this.pendingLine) {
        return
      }
      const code = (this.verifyCode || '').trim()
      if (!/^\d{6}$/.test(code)) {
        this.$message.warning('请输入6位数字验证码')
        return
      }
      this.confirmingVerify = true
      confirmLineVerifyCode({
        appId: this.pendingLine.appId,
        code
      }).then(() => {
        this.$message.success('验证通过')
        const line = this.pendingLine
        this.verifyDialogVisible = false
        this.pendingLine = null
        this.verifyCode = ''
        this.proceedSelectLine(line)
      }).catch(err => {
        this.$message.error(err.message || '验证码校验失败')
      }).finally(() => {
        this.confirmingVerify = false
      })
    },
    handleVerifyDialogClose() {
      this.verifyDialogVisible = false
      this.pendingLine = null
      this.verifyCode = ''
      this.clearSendCountdown()
    },
    startSendCountdown(seconds) {
      this.clearSendCountdown()
      this.sendCountdown = seconds
      this.countdownTimer = setInterval(() => {
        if (this.sendCountdown <= 1) {
          this.sendCountdown = 0
          this.clearSendCountdown()
        } else {
          this.sendCountdown--
        }
      }, 1000)
    },
    clearSendCountdown() {
      if (this.countdownTimer) {
        clearInterval(this.countdownTimer)
        this.countdownTimer = null
      }
      this.sendCountdown = 0
    },
    async proceedSelectLine(line) {
      if (this.detectingLineId) {
        this.$message.warning('正在检测其他线路，请稍候')
        return
      }

      this.detectingLineId = line.appId
      this.addLog('info', `开始检测线路: ${line.appName}`)
      this.addLog('info', `目标地址: ${line.host}:${line.srvPort}`)

      try {
        // 构建检测请求参数
        const detectParams = {
          host: line.host,
          srvPort: line.srvPort,
          spaPort: line.spaPort,
          spaKey: line.spaKey
        }

        this.addLog('info', '正在发送探测请求...')
        const serverData = await detectServer(detectParams)

        // 检查响应
        if (serverData && serverData.host && serverData.srvPort) {
          // 检查 available 字段
          if (serverData.available === true) {
            this.addLog('info', `服务器连通性检测成功`)
            this.addLog('info', `连接地址: ${serverData.host}:${serverData.srvPort}`)

            // 初始化选择服务器
            this.addLog('info', '正在初始化选择服务器...')
            try {
              const selectResult = await selectServer({
                host: line.host,
                srvPort: line.srvPort,
                spaPort: line.spaPort,
                spaKey: line.spaKey
              })

              if (selectResult && selectResult.code === '200' && selectResult.data) {
                this.addLog('info', `服务器初始化成功`)
                this.addLog('info', `服务器名称: ${selectResult.data.name || 'N/A'}`)
                this.addLog('info', `语言设置: ${selectResult.data.language?.desc || 'N/A'}`)

                // 获取服务器和客户端版本号
                this.addLog('info', '正在获取版本信息...')
                try {
                  // 并行获取服务器和客户端版本
                  const [serverVersionRes, clientVersionRes] = await Promise.all([
                    getServerVersion({
                      host: line.host,
                      srvPort: line.srvPort,
                      spaPort: line.spaPort,
                      spaKey: line.spaKey
                    }),
                    getClientVersion()
                  ])

                  // 打印服务器版本
                  if (serverVersionRes && serverVersionRes.code === '200' && serverVersionRes.data) {
                    this.addLog('info', `服务器版本: ${serverVersionRes.data.version || 'N/A'}`)
                    this.addLog('info', `服务器目标: ${serverVersionRes.data.target || 'N/A'}`)
                  }

                  // 打印客户端版本
                  if (clientVersionRes && clientVersionRes.code === '200' && clientVersionRes.data) {
                    this.addLog('info', `客户端名称: ${clientVersionRes.data.clientName || 'N/A'}`)
                    this.addLog('info', `客户端版本: ${clientVersionRes.data.localVersion || 'N/A'}`)
                    this.addLog('info', `客户端主版本: ${clientVersionRes.data.localMainVersion || 'N/A'}`)
                  }
                } catch (versionErr) {
                  this.addLog('error', `获取版本信息失败: ${versionErr.message || '未知错误'}`)
                  // 版本获取失败不影响主流程，继续执行
                }

                // 获取用户凭证并登录控制器
                this.addLog('info', '正在获取登录凭证...')
                try {
                  const credRes = await getUserCredentials(line.appId)
                  if (credRes.code === 200 && credRes.data) {
                    this.addLog('info', `用户: ${credRes.data.username}`)
                    this.addLog('info', '正在登录控制器...')
                    try {
                      const loginRes = await loginWithAccount(credRes.data.username, credRes.data.password)
                      if (loginRes && loginRes.code === '200' && loginRes.data) {
                        this.addLog('info', '控制器登录成功')
                        this.addLog('info', `用户名: ${loginRes.data.account || loginRes.data.name || 'N/A'}`)
                        this.addLog('info', '正在跳转到应用列表...')
                        await this.$store.dispatch('SelectLine', line)
                        clearPendingLine()
                        this.$store.commit('SET_PENDING_LINE', null)
                        this.$router.replace('/app-list')
                      } else {
                        this.addLog('error', '控制器登录失败: ' + (loginRes?.messages || '未知错误'))
                      }
                    } catch (loginErr) {
                      this.addLog('error', '控制器登录失败: ' + (loginErr.message || '未知错误'))
                    }
                  } else {
                    this.addLog('error', '获取凭证失败: ' + (credRes.msg || '凭证已过期，请重新登录'))
                  }
                } catch (credErr) {
                  this.addLog('error', '获取凭证失败: ' + (credErr.message || '未知错误'))
                }

                this.addLog('info', '线路选择完成')
              } else {
                this.addLog('error', '服务器初始化失败：返回数据无效')
                this.$message.error('服务器初始化失败，请重试')
              }
            } catch (selectErr) {
              this.addLog('error', `服务器初始化失败: ${selectErr.message || '未知错误'}`)
              this.$message.error('服务器初始化失败: ' + (selectErr.message || '未知错误'))
            }
          } else {
            this.addLog('error', `服务不可用 - ${serverData.host}:${serverData.srvPort}`)
            this.$message.error('服务器不可用，请选择其他线路')
          }
        } else {
          this.addLog('error', '服务器响应数据不完整')
          this.$message.error('服务器连接失败，请稍后重试')
        }
      } catch (err) {
        this.addLog('error', `检测失败: ${err.message || '未知错误'}`)
        this.$message.error('服务器连接失败: ' + (err.message || '未知错误'))
      } finally {
        this.detectingLineId = null
      }
    },
    addLog(type, message) {
      const now = new Date()
      const time = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`

      this.logs.push({
        type,
        time,
        message
      })

      // 自动滚动到底部
      this.$nextTick(() => {
        const logContent = this.$refs.logContent
        if (logContent) {
          logContent.scrollTop = logContent.scrollHeight
        }
      })
    },
    handleLogout() {
      removeToken()
      this.$store.dispatch('LogOut').then(() => {
        this.$router.replace('/choose-line')
      })
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
  width: 50vw;
  max-width: 1000px;
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

.content-wrapper {
  display: flex;
  gap: 24px;
}

.line-section {
  flex: 1;
  min-width: 0;
}

.log-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  background: #fafafa;
}

.log-section-full {
  flex: 1;
  width: 100%;
}

.log-header {
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 8px;
}

.log-header i {
  font-size: 16px;
}

.log-content {
  flex: 1;
  overflow-y: auto;
  max-height: 400px;
  min-height: 300px;
}

.log-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #c0c4cc;
  padding: 40px 20px;
}

.log-empty i {
  font-size: 32px;
  margin-bottom: 12px;
}

.log-empty p {
  margin: 0;
  font-size: 13px;
}

.log-list {
  padding: 12px;
}

.log-item {
  padding: 8px 12px;
  margin-bottom: 6px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.5;
  font-family: 'Consolas', 'Monaco', monospace;
  display: flex;
  gap: 8px;
}

.log-item.info {
  background: #ecf5ff;
  color: #409eff;
}

.log-item.success {
  background: #f0f9ff;
  color: #409eff;
}

.log-item.error {
  background: #fef0f0;
  color: #f56c6c;
}

.log-time {
  color: #909399;
  flex-shrink: 0;
}

.log-message {
  flex: 1;
  word-break: break-all;
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

.line-item.detecting {
  border-color: #409eff;
  background: #ecf5ff;
  cursor: not-allowed;
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

.verify-line-hint {
  margin: 0 0 16px;
  font-size: 14px;
  color: #606266;
}

.verify-code-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.verify-code-input {
  flex: 1;
}

.verify-help-icon {
  font-size: 18px;
  color: #909399;
  cursor: help;
  flex-shrink: 0;
}

.verify-tooltip-content {
  max-width: 280px;
  line-height: 1.5;
}
</style>
