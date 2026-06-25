import request from '@/utils/request'
import { parseStrEmpty } from '@/utils/ruoyi'

export function listLocalUser(query) {
  return request({
    url: '/yianlian/vpn/local/user/list',
    method: 'get',
    params: query
  })
}

export function getLocalUser(localUserId) {
  return request({
    url: '/yianlian/vpn/local/user/' + parseStrEmpty(localUserId),
    method: 'get'
  })
}

export function addLocalUser(data) {
  return request({
    url: '/yianlian/vpn/local/user',
    method: 'post',
    data: data
  })
}

export function updateLocalUser(data) {
  return request({
    url: '/yianlian/vpn/local/user',
    method: 'put',
    data: data
  })
}

export function delLocalUser(localUserId) {
  return request({
    url: '/yianlian/vpn/local/user/' + localUserId,
    method: 'delete'
  })
}

export function resetLocalUserPwd(localUserId, password) {
  return request({
    url: '/yianlian/vpn/local/user/resetPwd',
    method: 'put',
    data: { localUserId, password }
  })
}

export function changeLocalUserStatus(localUserId, status) {
  return request({
    url: '/yianlian/vpn/local/user/changeStatus',
    method: 'put',
    data: { localUserId, status }
  })
}

export function syncLocalUserToLine(data) {
  return request({
    url: '/yianlian/vpn/local/user/sync',
    method: 'post',
    data: data
  })
}

export function listLineUsers(localUserId) {
  return request({
    url: '/yianlian/vpn/local/user/line-users/' + localUserId,
    method: 'get'
  })
}

export function listOfflineLoginLines(localUserId) {
  return request({
    url: '/yianlian/vpn/local/user/offline-lines/' + localUserId,
    method: 'get'
  })
}

export function exportOfflineLogin(data) {
  return request({
    url: '/yianlian/vpn/local/user/offline-export',
    method: 'post',
    data: data,
    responseType: 'blob'
  })
}
