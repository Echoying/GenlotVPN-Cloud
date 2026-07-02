import request from '@/utils/request'

// 查询 VPN 在线用户列表
export function list(query) {
  return request({
    url: '/yianlian/vpn/online/list',
    method: 'get',
    params: query
  })
}

// 强退 VPN 在线用户
export function forceLogout(tokenId) {
  return request({
    url: '/yianlian/vpn/online/' + tokenId,
    method: 'delete'
  })
}
