import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi"

// 查询VPN用户列表
export function listUser(query) {
  return request({
    url: '/yianlian/vpn/user/list',
    method: 'get',
    params: query
  })
}

// 查询VPN用户详细
export function getUser(userId, appId) {
  return request({
    url: '/yianlian/vpn/user/' + parseStrEmpty(userId),
    method: 'get',
    params: { appId }
  })
}

// 新增VPN用户
export function addUser(data) {
  return request({
    url: '/yianlian/vpn/user',
    method: 'post',
    data: data
  })
}

// 修改VPN用户
export function updateUser(data) {
  return request({
    url: '/yianlian/vpn/user',
    method: 'put',
    data: data
  })
}

// 删除VPN用户
export function delUser(userId) {
  return request({
    url: '/yianlian/vpn/user/' + userId,
    method: 'delete'
  })
}

// VPN用户密码重置
export function resetUserPwd(userId, password) {
  const data = {
    userId,
    password
  }
  return request({
    url: '/yianlian/vpn/user/resetPwd',
    method: 'put',
    data: data
  })
}

// VPN用户状态修改
export function changeUserStatus(userId, status) {
  const data = {
    userId,
    status
  }
  return request({
    url: '/yianlian/vpn/user/changeStatus',
    method: 'put',
    data: data
  })
}

// 查询VPN用户个人信息
export function getUserProfile() {
  return request({
    url: '/yianlian/vpn/user/profile',
    method: 'get'
  })
}

// 修改VPN用户个人信息
export function updateUserProfile(data) {
  return request({
    url: '/yianlian/vpn/user/profile',
    method: 'put',
    data: data
  })
}

// 用户密码重置
export function updateUserPwd(oldPassword, newPassword) {
  const data = {
    oldPassword,
    newPassword
  }
  return request({
    url: '/yianlian/vpn/user/profile/updatePwd',
    method: 'put',
    params: data
  })
}

// 用户头像上传
export function uploadAvatar(data) {
  return request({
    url: '/yianlian/vpn/user/profile/avatar',
    method: 'post',
    data: data
  })
}

// 查询授权角色
export function getAuthRole(userId) {
  return request({
    url: '/yianlian/vpn/user/authRole/' + userId,
    method: 'get'
  })
}

// 保存授权角色
export function updateAuthRole(data) {
  return request({
    url: '/yianlian/vpn/user/authRole',
    method: 'put',
    params: data
  })
}

// 查询部门下拉树结构
export function deptTreeSelect(params) {
  return request({
    url: '/yianlian/vpn/user/deptTree',
    method: 'get',
    params: params
  })
}
