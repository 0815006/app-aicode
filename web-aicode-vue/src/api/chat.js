import request from '@/utils/request'

export function loadHistoryMessages(params) {
  return request({
    url: '/api/chat/history',
    method: 'get',
    params
  })
}

export function sendMessage(data) {
  return request({
    url: '/api/chat/send',
    method: 'post',
    data
  })
}
