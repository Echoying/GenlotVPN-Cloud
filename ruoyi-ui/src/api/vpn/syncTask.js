import request from '@/utils/request'

// 查询同步任务列表
export function listSyncTask(query) {
  return request({
    url: '/yianlian/synctask/list',
    method: 'get',
    params: query
  })
}

// 查询同步任务详情
export function getSyncTask(taskId) {
  return request({
    url: '/yianlian/synctask/' + taskId,
    method: 'get'
  })
}
