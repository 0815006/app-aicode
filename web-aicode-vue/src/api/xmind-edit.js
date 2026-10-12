// src/api/xmind-edit.js
// XMind 编辑器接口（独立于旧的 xmind.js，纯文件存储，按工号分目录）
import request from '@/utils/request'

/**
 * 列出当前用户的 XMind 文件
 * @returns {Promise} [{ name, size, lastModified }]
 */
export function listXmindFiles() {
  return request({
    url: '/xmind-edit/list',
    method: 'get'
  })
}

/**
 * 新建空白 XMind 文件
 * @param {String} fileName 文件名（可不带 .xmind 后缀）
 * @returns {Promise}
 */
export function createXmind(fileName) {
  return request({
    url: '/xmind-edit/create',
    method: 'post',
    data: { fileName }
  })
}

/**
 * 上传 XMind 文件到当前用户目录
 * @param {File} file
 * @returns {Promise}
 */
export function uploadXmind(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/xmind-edit/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 下载当前用户的 XMind 文件（返回 Blob，供预览解析或本地保存）
 * @param {String} fileName
 * @returns {Promise} axios response
 */
export function downloadXmind(fileName) {
  return request({
    url: '/xmind-edit/download',
    method: 'get',
    params: { fileName },
    responseType: 'blob'
  })
}

/**
 * 保存（覆盖）当前用户的 XMind 文件
 * @param {String} fileName
 * @param {Blob} blob
 * @returns {Promise}
 */
export function saveXmind(fileName, blob) {
  const formData = new FormData()
  formData.append('file', blob, fileName)
  return request({
    url: '/xmind-edit/save',
    method: 'post',
    params: { fileName },
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 删除当前用户的 XMind 文件
 * @param {String} fileName
 * @returns {Promise}
 */
export function deleteXmind(fileName) {
  return request({
    url: '/xmind-edit/delete',
    method: 'post',
    params: { fileName }
  })
}
