import request from '@/utils/request'

// 查询参数列表
export function listLineApp(query) {
  return request({
    url: '/yianlian/line/list',
    method: 'get',
    params: query
  })
}

// 查询参数详细
export function getLineApp(appId) {
  return request({
    url: '/yianlian/line/' + appId,
    method: 'get'
  })
}

// 新增参数配置
export function addLineApp(data) {
  return request({
    url: '/yianlian/line',
    method: 'post',
    data: data
  })
}

// 修改参数配置
export function updateLineApp(data) {
  return request({
    url: '/yianlian/line',
    method: 'put',
    data: data
  })
}

// 删除参数配置
export function delLineApp(appId) {
  return request({
    url: '/yianlian/line/' + appId,
    method: 'delete'
  })
}

