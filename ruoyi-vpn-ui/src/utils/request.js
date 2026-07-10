import axios from 'axios'
import { getToken } from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import { tansParams } from '@/utils/ruoyi'
import { Message, MessageBox } from 'element-ui'

const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  // 超时（修改密码等需同步易安联，先登录代理，故放宽到180s）
  timeout: 180000
})

service.interceptors.request.use(config => {
  const isToken = (config.headers || {}).isToken === false
  if (getToken() && !isToken) {
    config.headers['Authorization'] = 'Bearer ' + getToken()
  }
  if (config.method === 'get' && config.params) {
    let url = config.url + '?' + tansParams(config.params)
    url = url.slice(0, -1)
    config.params = {}
    config.url = url
  }
  return config
}, error => {
  return Promise.reject(error)
})

service.interceptors.response.use(res => {
  const code = res.data.code || 200
  const msg = errorCode[code] || res.data.msg || errorCode['default']
  if (code === 401) {
    // 登录接口本身返回401（如令牌不能为空）不弹窗，直接reject
    if (res.config && res.config.url && res.config.url.includes('/vpn/login')) {
      return Promise.reject(new Error(msg))
    }
    MessageBox.confirm('登录状态已过期，您可以继续留在该页面，或者重新登录', '系统提示', {
      confirmButtonText: '重新登录',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      import('@/store').then(({ default: store }) => {
        store.dispatch('user/LogOut').then(() => {
          location.href = '/login'
        })
      })
    })
    return Promise.reject(new Error(msg))
  } else if (code === 500) {
    Message({ message: msg, type: 'error' })
    return Promise.reject(new Error(msg))
  } else if (code !== 200) {
    Message({ message: msg, type: 'error' })
    return Promise.reject(new Error(msg))
  }
  return res.data
}, error => {
  let { message } = error
  if (message === 'Network Error') {
    message = '后端接口连接异常'
  } else if (message.includes('timeout')) {
    message = '系统接口请求超时'
  } else if (message.includes('Request failed with status code')) {
    message = '系统接口' + message.substr(message.length - 3) + '异常'
  }
  Message({ message, type: 'error', duration: 5 * 1000 })
  return Promise.reject(error)
})

export default service
