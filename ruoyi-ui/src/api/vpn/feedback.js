import request from '@/utils/request'

export function listFeedback(query) {
  return request({ url: '/yianlian/vpn/feedback/list', method: 'get', params: query })
}
export function getFeedback(id) {
  return request({ url: '/yianlian/vpn/feedback/' + id, method: 'get' })
}
export function updateFeedbackStatus(data) {
  return request({ url: '/yianlian/vpn/feedback', method: 'put', data })
}
