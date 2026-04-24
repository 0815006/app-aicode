// src/api/resource.js
import request from '@/utils/request'

/**
 * 核查资源：按产品ID查询
 * @param {string} productId
 * @param {string} fileSource
 * @returns {Promise}
 */
export function checkResources(productId, fileSource) {
  return request.get('/resource/check', {
    params: { productId: productId, fileSource: fileSource }
  })
}

/**
 * 获取所有产品ID列表
 * @returns {Promise}
 */
export function getProductIds() {
  return request.get('/resource/productIds')
}
/**
 * 上传环境资源清单 Excel 文件
 * @param {string} productId - 产品标识
 * @param {File} file - 上传的 Excel 文件
 * @param {string} fileSource - 文件来源
 * @returns {Promise}
 */
export function uploadResource(productId, file, fileSource) {
  const formData = new FormData()
  formData.append('productId', productId)
  formData.append('file', file)
  formData.append('fileSource', fileSource)

  return request.post('/resource/uploadResource', formData, {
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
