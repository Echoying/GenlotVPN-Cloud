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

// 响应拦截器
controllerService.interceptors.response.use(
  response => {
    const res = response.data
    const code = res.code

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
      // 检查 data 是否存在且有内容
      if (!res.data || (Array.isArray(res.data) && res.data.length === 0)) {
        Message.warning('未返回有效数据')
        return res // 仍然返回，让调用方处理
      }

      // 如果是数组，检查每个元素是否有 host 和 srvPort
      if (Array.isArray(res.data)) {
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
  })
}

export default controllerService
