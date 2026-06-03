import request from '@/utils/request'

// 发送选线钉钉验证码
export function sendLineVerifyCode(data) {
  return request({
    url: '/vpn/line-verify/send',
    method: 'post',
    data
  })
}

// 校验选线验证码
export function confirmLineVerifyCode(data) {
  return request({
    url: '/vpn/line-verify/confirm',
    method: 'post',
    data
  })
}
