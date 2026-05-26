import request from '@/utils/request'

// 登录方法
export function login(username, password, code, uuid) {
  const data = { username, password, code, uuid }
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
