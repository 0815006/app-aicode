import request from '@/utils/request'

export function loadHistoryMessages(params) {
  return request({
    url: '/chat/history',
    method: 'get',
    params
  })
}

export function sendMessage(data) {
  return request({
    url: '/chat/send',
    method: 'post',
    data
  })
}
