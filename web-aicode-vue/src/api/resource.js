// src/api/resource.js
import request from '@/utils/request'

/**
 * 核查资源：按产品ID查询
 * @param {string} productId
 * @returns {Promise}
 */
export function checkResources(productId) {
  return request.get('/resource/check', {
    params: { productId: productId }
  })
}
/**
 * 上传环境资源清单 Excel 文件
 * @param {string} productId - 产品标识
 * @param {File} file - 上传的 Excel 文件
 * @returns {Promise}
 */
export function uploadResource(productId, file) {
  const formData = new FormData()
  formData.append('productId', productId)
  formData.append('file', file)

  return request.post('/performance/uploadResource', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 删除指定产品下、指定原始文件名的资源数据
 * @param {string} productId
 * @param {string} originalFileName
 * @returns {Promise}
 */
export function deleteResourceByFile(productId, originalFileName) {
  return request.delete('/resource/deleteByFile', {
    params: { productId, originalFileName }
  })
}
