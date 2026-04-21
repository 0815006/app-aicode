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

export function getFileList() {
  return request({
    url: '/chat/file/list',
    method: 'get'
  })
}

export function deleteFile(id, uploaderId) {
  return request({
    url: `/chat/file/delete/${id}`,
    method: 'delete',
    params: { uploaderId }
  })
}

export function getDownloadUrl(id) {
  return `/api/chat/file/download/${id}`
}
