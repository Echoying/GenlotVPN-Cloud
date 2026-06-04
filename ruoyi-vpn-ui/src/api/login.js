import request from '@/utils/request'

// 登录方法
export function login(username, password, code, uuid, appId) {
  const data = { username, password, code, uuid, appId }
  return request({
    url: '/vpn/login',
    headers: { isToken: false },
    method: 'post',
    data: data
  })
}

// 获取验证码
export function getCodeImg() {
  return request({
    url: '/code?t=' + Date.now(),
    headers: { isToken: false },
    method: 'get',
    timeout: 20000
  })
}

// 修改密码
export function changePassword(data) {
  return request({
    url: '/vpn/change-password',
    headers: { isToken: false },
    method: 'put',
    data: data
  })
}
