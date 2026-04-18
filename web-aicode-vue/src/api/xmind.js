import request from '@/utils/request'

/**
 * 后端转换 XMind 文件
 * @param {File} file 
 * @returns {Promise}
 */
export function convertXmind(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/xmind/convert',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
