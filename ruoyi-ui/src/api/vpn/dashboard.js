import request from '@/utils/request'

export function getOverview() {
  return request({
    url: '/yianlian/vpn/dashboard/overview',
    method: 'get'
  })
}

export function getLoginTrend(days) {
  return request({
    url: '/yianlian/vpn/dashboard/login-trend',
    method: 'get',
    params: { days }
  })
}

export function getDistributions(days) {
  return request({
    url: '/yianlian/vpn/dashboard/distributions',
    method: 'get',
    params: { days }
  })
}

export function getRecentEvents(limit) {
  return request({
    url: '/yianlian/vpn/dashboard/recent-events',
    method: 'get',
    params: { limit }
  })
}
