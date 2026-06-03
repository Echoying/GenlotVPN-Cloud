import axios from 'axios'
import { Message } from 'element-ui'

// 创建专用于本地控制器的 axios 实例
const controllerService = axios.create({
  baseURL: 'http://127.0.0.1:30303',
  timeout: 120000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 不需要校验 data 的接口路径（允许 data 为 null）
const ALLOW_NULL_DATA_URLS = ['/api/v1/user/logout', '/api/v1/gateway/turnOn', '/api/v1/gateway/switch']
// 允许 data 为空数组且不弹「未返回有效数据」
const ALLOW_EMPTY_ARRAY_URLS = ['/api/v1/user/getUserGroupedServiceList']
// 数组响应不做 host/srvPort 探测字段校验
const SKIP_ARRAY_ITEM_VALIDATION_URLS = ['/api/v1/user/getUserGroupedServiceList']

// 响应拦截器
controllerService.interceptors.response.use(
  response => {
    const res = response.data
    const code = res.code
    const requestUrl = response.config.url || ''

    // 201: JSON解析失败
    if (code === '201') {
      Message.error(res.messages || 'JSON解析失败')
      return Promise.reject(new Error(res.messages || 'JSON解析失败'))
    }

    // 202: 控制器信息为空
    if (code === '202') {
      Message.error('控制器信息为空，请检查网络连接')
      return Promise.reject(new Error('no_network_conn'))
    }

    // 200 成功 - 但需要验证 data 是否有效
    if (code === '200') {
      const skipNullValidation = ALLOW_NULL_DATA_URLS.some(url => requestUrl.includes(url))
      if (skipNullValidation) {
        return res
      }

      const skipEmptyArray = ALLOW_EMPTY_ARRAY_URLS.some(url => requestUrl.includes(url))
      if (Array.isArray(res.data) && res.data.length === 0 && skipEmptyArray) {
        return res
      }

      // 检查 data 是否存在且有内容
      if (!res.data || (Array.isArray(res.data) && res.data.length === 0)) {
        Message.warning('未返回有效数据')
        return res
      }

      // 探测接口数组项校验 host/srvPort
      const skipArrayItemValidation = SKIP_ARRAY_ITEM_VALIDATION_URLS.some(url => requestUrl.includes(url))
      if (Array.isArray(res.data) && !skipArrayItemValidation) {
        const invalidItems = res.data.filter(item => !item.host || !item.srvPort)
        if (invalidItems.length > 0) {
          console.warn('部分数据缺少 host 或 srvPort:', invalidItems)
        }
      }

      return res
    }

    // 其他错误
    Message.error(res.messages || '请求失败')
    return Promise.reject(new Error(res.messages || '请求失败'))
  },
  error => {
    const requestUrl = (error.config && error.config.url) || ''

    // logout 接口：HTTP状态码非200但响应体code为200时，视为成功
    if (requestUrl.includes('/api/v1/user/logout') && error.response && error.response.data) {
      const res = error.response.data
      if (res.code === '200') {
        return res
      }
    }

    let message = '本地控制器连接失败'

    if (error.message.includes('Network Error')) {
      message = '无法连接到本地控制器(127.0.0.1:30303)，请确保控制器服务已启动'
    } else if (error.message.includes('timeout')) {
      message = '本地控制器请求超时'
    } else if (error.code === 'ECONNREFUSED') {
      message = '本地控制器服务未启动'
    }

    Message.error(message)
    return Promise.reject(error)
  }
)

/**
 * 探测服务器连通性
 * @param {Array} servers - 服务器列表
 * @param {string} servers[].host - 服务器的域名或IP
 * @param {string} servers[].srvPort - 服务器端口号
 * @param {string} servers[].spaPort - 敲门端口
 * @param {string} servers[].spaKey - 预共享秘钥(MD5加密32位小写)
 * @param {boolean} servers[].enablePortMapping - 是否启用端口映射，默认false
 * @param {string} servers[].mappingPort - 映射端口，默认和srvPort相同
 * @param {boolean} servers[].device_spa_enable - 是否开启设备敲门，默认false
 * @returns {Promise}
 */
export function detectServers(servers) {
  return controllerService({
    url: '/api/v1/control/detect',
    method: 'post',
    data: servers
  })
}

/**
 * 探测单个服务器连通性（便捷方法）
 * @param {Object} server - 服务器信息
 * @returns {Promise}
 */
export function detectServer(server) {
  const serverConfig = {
    host: server.host,
    srvPort: String(server.srvPort),
    spaPort: String(server.spaPort),
    spaKey: server.spaKey,
    enablePortMapping: server.enablePortMapping || false,
    mappingPort: String(server.srvPort),
    device_spa_enable: server.device_spa_enable || false
  }

  return detectServers([serverConfig]).then(res => {
    // 返回第一个结果
    return res.data && res.data.length > 0 ? res.data[0] : null
  })
}

/**
 * 初始化选择服务器
 * @param {Object} server - 服务器信息
 * @param {string} server.host - 服务器的域名或IP
 * @param {string} server.srvPort - 服务器端口号
 * @param {string} server.spaPort - 敲门端口
 * @param {string} server.spaKey - 预共享秘钥(MD5加密32位小写)
 * @returns {Promise} 返回 { code, messages, data: { name, version, language, AVSSwitch, IOASwitch, JiangminSwitch, LvaSwitch } }
 */
export function selectServer(server) {
  return controllerService({
    url: '/api/v1/control/select',
    method: 'post',
    data: {
      host: server.host,
      srvPort: String(server.srvPort),
      spaPort: String(server.spaPort),
      spaKey: server.spaKey,
      enablePortMapping: server.enablePortMapping || false,
      mappingPort: String(server.srvPort),
      device_spa_enable: server.device_spa_enable || false
    }
  }).then(res => {
    if (res.code === '200') {
      if (!res.data || typeof res.data !== 'object' || !res.data.language) {
        Message.error('服务器选择失败：返回数据无效')
        return Promise.reject(new Error('服务器选择失败：返回数据缺少language字段'))
      }
    }
    return res
  })
}

/**
 * 获取控制器信息版本号
 * @param {Object} server - 服务器信息
 * @param {string} server.host - 服务器的域名或IP
 * @param {string} server.srvPort - 服务器端口号
 * @param {string} server.spaPort - 敲门端口
 * @param {string} server.spaKey - 预共享秘钥(MD5加密32位小写)
 * @returns {Promise} 返回 { code, messages, data: { version, target, fullVersion } }
 */
export function getServerVersion(server) {
  return controllerService({
    url: '/api/v1/version/latestServer',
    method: 'post',
    data: {
      host: server.host,
      srvPort: String(server.srvPort),
      spaPort: String(server.spaPort),
      spaKey: server.spaKey,
      enablePortMapping: server.enablePortMapping || false,
      mappingPort: String(server.srvPort),
      device_spa_enable: server.device_spa_enable || false
    }
  }).then(res => {
    if (res.code === '200') {
      if (!res.data || typeof res.data !== 'object' || !res.data.version) {
        Message.error('获取服务器版本失败：返回数据无效')
        return Promise.reject(new Error('获取服务器版本失败：返回数据缺少version字段'))
      }
    }
    return res
  })
}

/**
 * 获取客户端版本号
 * @returns {Promise} 返回 { code, messages, data: { clientName, localVersion, localMainVersion } }
 */
export function getClientVersion() {
  return controllerService({
    url: '/api/v1/version/current',
    method: 'get'
  }).then(res => {
    if (res.code === '200') {
      if (!res.data || typeof res.data !== 'object' || !res.data.localMainVersion) {
        Message.error('获取客户端版本失败：返回数据无效')
        return Promise.reject(new Error('获取客户端版本失败：返回数据缺少localMainVersion字段'))
      }
    }
    return res
  })
}

/**
 * 账密登录
 * @param {string} username - 用户名
 * @param {string} password - 加密后密码(AES加密,CBC/pkcs7padding/128位,密钥:EnSwordAgent@123,偏移量:321@tnegAdrowSnE)
 * @returns {Promise} 返回 { code, messages, data: { token, userId, account, name, redirect, refreshToken } }
 */
export function loginWithAccount(username, password) {
  return controllerService({
    url: '/api/v1/user/loginWithAccount',
    method: 'post',
    data: {
      username,
      password
    }
  }).then(res => {
    if (res.code === '200') {
      if (!res.data || typeof res.data !== 'object' || !res.data.token) {
        Message.error('登录失败：返回数据无效')
        return Promise.reject(new Error('登录失败：返回数据缺少token字段'))
      }
    }
    return res
  })
}

/**
 * 用户登出
 * @returns {Promise} 返回 { code, messages, data }
 */
export function logout() {
  return controllerService({
    url: '/api/v1/user/logout',
    method: 'post'
  })
}

/**
 * 获取用户信息
 * @returns {Promise} 返回 { code, messages, data: { token, userId, account, name, redirect } }
 */
export function getUserInfo() {
  return controllerService({
    url: '/api/v1/user/info',
    method: 'get'
  })
}

/**
 * 获取应用列表（按组扁平列表，前端自行分组）
 * @param {string} serviceName 应用名称，为空则查询全部
 * @returns {Promise} 返回 { code, messages, data: Array } 应用项列表
 */
export function getUserGroupedServiceList(serviceName = '') {
  return controllerService({
    url: '/api/v1/user/getUserGroupedServiceList',
    method: 'post',
    data: {
      serviceName: serviceName || ''
    }
  }).then(res => {
    if (res.code === '200' && !Array.isArray(res.data)) {
      return Promise.reject(new Error('获取应用列表失败：返回数据格式错误'))
    }
    return res
  })
}

// 网关列表轮询配置
const GATEWAY_POLL_MAX = 20        // 最大轮询次数
const GATEWAY_POLL_INTERVAL = 3000 // 轮询间隔(ms)

/**
 * 获取网关列表
 * 登录后轮询调用，获取网关信息及连接状态
 * @returns {Promise} 返回 { code, messages, data } 其中 data 包含：
 *   - baselineIsMeet {boolean} 基线是否通过
 *   - tunCode {number} 隧道响应码(200隧道连接成功)
 *   - tunDesc {string} 隧道响应消息
 *   - turnOn {boolean} 开关
 *   - list {Array} 网关列表，元素含：
 *       id, name, srcIP, virtualIP, virtualIPv6,
 *       connected {boolean}, timing {string},
 *       canConnect {boolean}, delayTime {string}
 */
export function getGatewayList() {
  return controllerService({
    url: '/api/v1/gateway/list',
    method: 'get'
  }).then(res => {
    if (res.code === '200') {
      if (!res.data || typeof res.data !== 'object') {
        return Promise.reject(new Error('获取网关列表失败：返回数据缺少data字段'))
      }
    }
    return res
  })
}

/**
 * 打开/关闭网关连接
 * 登录后调用；登录后默认不开启网关，需显式打开
 * @param {boolean} turnOn true 打开连接，false 关闭连接
 * @returns {Promise} 返回 { code, messages, data }，成功时 data 可为 null
 */
export function turnOnGateway(turnOn) {
  return controllerService({
    url: '/api/v1/gateway/turnOn',
    method: 'post',
    data: {
      turnOn: !!turnOn
    }
  })
}

/**
 * 切换网关连接
 * 登录后调用，切换到指定网关
 * @param {string} gatewayID 网关 id（来自网关列表 list[].id）
 * @returns {Promise} 返回 { code, messages, data }，成功时 data 可为 null
 */
export function switchGateway(gatewayID) {
  return controllerService({
    url: '/api/v1/gateway/switch',
    method: 'post',
    data: {
      gatewayID
    }
  })
}

/**
 * 获取隧道连接状态
 * 登录后调用，用于展示隧道连接状态
 * @returns {Promise} 返回 { code, messages, data: { status, reConnect } }
 *   status: 0未连接 1连接中 2已连接 3断开连接中 4已断开
 */
export function getTunnelStatus() {
  return controllerService({
    url: '/api/v1/tunnel/status',
    method: 'get'
  }).then(res => {
    if (res.code === '200') {
      if (!res.data || typeof res.data !== 'object') {
        return Promise.reject(new Error('获取隧道状态失败：返回数据缺少data字段'))
      }
    }
    return res
  })
}

/**
 * 轮询获取网关列表
 * 最多轮询 GATEWAY_POLL_MAX 次，每次间隔 GATEWAY_POLL_INTERVAL 毫秒
 * @param {Object} options
 * @param {Function} [options.onUpdate] 每次拿到数据的回调 (data, attempt) => void
 * @param {Function} [options.isDone]   提前结束判断 (data) => boolean，返回 true 立即结束
 * @returns {Promise} resolve(最后一次 data)；达到上限仍未满足或异常则 reject
 */
export function pollGatewayList({ onUpdate, isDone } = {}) {
  return new Promise((resolve, reject) => {
    let attempts = 0
    const tick = () => {
      attempts++
      getGatewayList().then(res => {
        const data = res.data
        if (onUpdate) onUpdate(data, attempts)
        if (isDone && isDone(data)) return resolve(data)
        if (attempts >= GATEWAY_POLL_MAX) return reject(new Error('网关连接超时'))
        setTimeout(tick, GATEWAY_POLL_INTERVAL)
      }).catch(err => {
        if (attempts >= GATEWAY_POLL_MAX) return reject(err)
        setTimeout(tick, GATEWAY_POLL_INTERVAL)
      })
    }
    tick()
  })
}

export default controllerService
