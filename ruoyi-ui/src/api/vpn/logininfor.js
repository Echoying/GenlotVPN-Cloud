import request from '@/utils/request'

// 查询 VPN 登录日志列表
export function list(query) {
  return request({
    url: '/yianlian/vpnlogininfor/list',
    method: 'get',
    params: query
  })
}

// 删除 VPN 登录日志
export function delLogininfor(infoId) {
  return request({
    url: '/yianlian/vpnlogininfor/' + infoId,
    method: 'delete'
  })
}

// 清空 VPN 登录日志
export function cleanLogininfor() {
  return request({
    url: '/yianlian/vpnlogininfor/clean',
    method: 'delete'
  })
}
