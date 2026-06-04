import request from '@/utils/request'

// 登录前获取可选线路列表（无需 token）
export function getPublicLines() {
  return request({
    url: '/vpn/lines',
    headers: { isToken: false },
    method: 'get'
  })
}

// 获取当前用户的授权线路列表
export function getAuthorizedLines() {
  return request({
    url: '/vpn/authorized-lines',
    method: 'get'
  })
}

// 获取当前用户的控制器登录凭据（用户名 + AES加密后的密码）
export function getUserCredentials(appId) {
  return request({
    url: '/vpn/user-credentials',
    method: 'get',
    params: { appId }
  })
}
