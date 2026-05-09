import request from '@/utils/request'

// 查询VPN角色列表
export function listRole(query) {
  return request({
    url: '/yianlian/vpn/role/list',
    method: 'get',
    params: query
  })
}

// 查询VPN角色详细
export function getRole(roleId) {
  return request({
    url: '/yianlian/vpn/role/' + roleId,
    method: 'get'
  })
}

// 新增VPN角色
export function addRole(data) {
  return request({
    url: '/yianlian/vpn/role',
    method: 'post',
    data: data
  })
}

// 修改VPN角色
export function updateRole(data) {
  return request({
    url: '/yianlian/vpn/role',
    method: 'put',
    data: data
  })
}

// VPN角色数据权限
export function dataScope(data) {
  return request({
    url: '/yianlian/vpn/role/dataScope',
    method: 'put',
    data: data
  })
}

// VPN角色状态修改
export function changeRoleStatus(roleId, status) {
  const data = {
    roleId,
    status
  }
  return request({
    url: '/yianlian/vpn/role/changeStatus',
    method: 'put',
    data: data
  })
}

// 删除VPN角色
export function delRole(roleId) {
  return request({
    url: '/yianlian/vpn/role/' + roleId,
    method: 'delete'
  })
}

// 根据角色ID查询部门树结构
export function deptTreeSelect(roleId) {
  return request({
    url: '/yianlian/vpn/role/deptTree/' + roleId,
    method: 'get'
  })
}
