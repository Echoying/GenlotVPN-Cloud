import request from '@/utils/request'

// 获取当前用户的授权线路列表
export function getAuthorizedLines() {
  return request({
    url: '/vpn/authorized-lines',
    method: 'get'
  })
}

// 获取当前用户的控制器登录凭据（用户名 + AES加密后的密码）
export function getUserCredentials() {
  return request({
    url: '/vpn/user-credentials',
    method: 'get'
  })
}
