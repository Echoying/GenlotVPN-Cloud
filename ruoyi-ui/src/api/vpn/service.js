import request from '@/utils/request'

export function listService(query) {
  return request({ url: '/yianlian/vpn/service/list', method: 'get', params: query })
}

export function getService(id) {
  return request({ url: '/yianlian/vpn/service/' + id, method: 'get' })
}

export function addService(data) {
  return request({ url: '/yianlian/vpn/service', method: 'post', data: data })
}

export function updateService(data) {
  return request({ url: '/yianlian/vpn/service', method: 'put', data: data })
}

export function delService(ids) {
  return request({ url: '/yianlian/vpn/service/' + ids, method: 'delete' })
}
