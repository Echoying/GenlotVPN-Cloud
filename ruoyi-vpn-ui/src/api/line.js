import request from '@/utils/request'

// 获取当前用户的授权线路列表
export function getAuthorizedLines() {
  return request({
    url: '/vpn/authorized-lines',
    method: 'get'
  })
}
