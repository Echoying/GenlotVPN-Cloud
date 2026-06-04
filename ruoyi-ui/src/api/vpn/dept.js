import request from '@/utils/request'

// 查询VPN部门列表
export function listDept(query) {
  return request({
    url: '/yianlian/vpn/dept/list',
    method: 'get',
    params: query
  })
}

// 查询VPN部门列表（排除节点）
export function listDeptExcludeChild(deptId, params) {
  return request({
    url: '/yianlian/vpn/dept/list/exclude/' + deptId,
    method: 'get',
    params: params
  })
}

// 查询VPN部门详细
export function getDept(deptId) {
  return request({
    url: '/yianlian/vpn/dept/' + deptId,
    method: 'get'
  })
}

// 查询VPN部门下拉树结构
export function treeselect() {
  return request({
    url: '/yianlian/vpn/dept/treeselect',
    method: 'get'
  })
}

// 根据角色ID查询VPN部门树结构
export function roleDeptTreeselect(roleId) {
  return request({
    url: '/yianlian/vpn/dept/roleDeptTreeselect/' + roleId,
    method: 'get'
  })
}

// 新增VPN部门
export function addDept(data) {
  return request({
    url: '/yianlian/vpn/dept',
    method: 'post',
    data: data
  })
}

// 修改VPN部门
export function updateDept(data) {
  return request({
    url: '/yianlian/vpn/dept',
    method: 'put',
    data: data
  })
}

// 保存VPN部门排序
export function updateDeptSort(data) {
  return request({
    url: '/yianlian/vpn/dept/updateSort',
    method: 'put',
    data: data
  })
}

// 删除VPN部门
export function delDept(deptId) {
  return request({
    url: '/yianlian/vpn/dept/' + deptId,
    method: 'delete'
  })
}
