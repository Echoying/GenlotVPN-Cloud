import request from '@/utils/request'

// 根据部门ID查询授权列表
export function listByDeptId(deptId) {
  return request({
    url: '/yianlian/deptAuth/listByDeptId/' + deptId,
    method: 'get'
  })
}

// 批量保存部门授权
export function batchSave(data) {
  return request({
    url: '/yianlian/deptAuth/batchSave',
    method: 'post',
    data: data
  })
}

// 获取应用服务树（应用组+应用，按线路）
export function getServiceTree(appId) {
  return request({
    url: '/yianlian/deptAuth/serviceTree',
    method: 'get',
    params: { appId }
  })
}
