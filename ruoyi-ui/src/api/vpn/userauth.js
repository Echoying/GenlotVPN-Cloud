import request from '@/utils/request'

// 根据用户ID查询授权列表
export function listByUserId(userId, lineId) {
  return request({
    url: '/yianlian/userAuth/listByUserId/' + userId,
    method: 'get',
    params: { lineId }
  })
}

// 批量保存用户授权
export function batchSaveUserAuth(data) {
  return request({
    url: '/yianlian/userAuth/batchSave',
    method: 'post',
    data: data
  })
}

// 获取应用服务树（应用组+应用，按线路）
export function getServiceTree(appId) {
  return request({
    url: '/yianlian/userAuth/serviceTree',
    method: 'get',
    params: { appId }
  })
}
