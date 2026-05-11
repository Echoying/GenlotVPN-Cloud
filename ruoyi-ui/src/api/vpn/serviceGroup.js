import request from '@/utils/request'

// 查询应用组列表
export function listServiceGroup(query) {
  return request({
    url: '/yianlian/vpn/serviceGroup/list',
    method: 'get',
    params: query
  })
}

// 查询应用组详细
export function getServiceGroup(id) {
  return request({
    url: '/yianlian/vpn/serviceGroup/' + id,
    method: 'get'
  })
}

// 新增应用组
export function addServiceGroup(data) {
  return request({
    url: '/yianlian/vpn/serviceGroup',
    method: 'post',
    data: data
  })
}

// 修改应用组
export function updateServiceGroup(data) {
  return request({
    url: '/yianlian/vpn/serviceGroup',
    method: 'put',
    data: data
  })
}

// 删除应用组
export function delServiceGroup(id) {
  return request({
    url: '/yianlian/vpn/serviceGroup/' + id,
    method: 'delete'
  })
}

// 查询应用组下拉树
export function serviceGroupTreeselect(query) {
  return request({
  url: '/yianlian/vpn/serviceGroup/treeselect',
    method: 'get',
    params: query
  })
}

// 查询应用组列表（排除节点）
export function listServiceGroupExcludeChild(id) {
  return request({
    url: '/yianlian/vpn/serviceGroup/list/exclude/' + id,
    method: 'get'
  })
}
