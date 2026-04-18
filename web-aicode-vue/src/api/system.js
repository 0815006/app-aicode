import request from '@/utils/request'

/**
 * 获取客户端 IP
 * @returns {Promise}
 */
export function getClientIp() {
  return request.get('/system/get-ip')
}
