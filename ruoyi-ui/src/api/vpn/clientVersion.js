import request from '@/utils/request'

// 查询客户端版本策略
export function getClientVersionPolicy() {
  return request({
    url: '/yianlian/vpn/clientVersion',
    method: 'get'
  })
}

// 更新客户端版本策略
export function updateClientVersionPolicy(data) {
  return request({
    url: '/yianlian/vpn/clientVersion',
    method: 'put',
    data: data
  })
}
