import request from '@/utils/request'

// 子目录管理
export function listDirectories() {
  return request({
    url: '/file-share/directories',
    method: 'get'
  })
}

export function createDirectory(name) {
  return request({
    url: '/file-share/directory',
    method: 'post',
    params: { name }
  })
}

export function deleteDirectory(name) {
  return request({
    url: '/file-share/directory',
    method: 'delete',
    params: { name }
  })
}

// 文件管理
export function listFiles(subDirectoryName) {
  return request({
    url: '/file-share/files',
    method: 'get',
    params: { subDirectoryName }
  })
}

export function deleteFile(id, uploaderId) {
  return request({
    url: `/file-share/delete/${id}`,
    method: 'delete',
    params: { uploaderId }
  })
}

export function getDownloadUrl(id) {
  return `/api/file-share/download/${id}`
}
