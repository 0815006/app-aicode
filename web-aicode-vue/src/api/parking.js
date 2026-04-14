// src/api/parking.js
import request from '@/utils/request'

/**
 * 获取所有车位预约记录
 * @returns {Promise}
 */
export function getParkingList() {
  return request.get('/parking/book/list')
}

// 新增
export function createParking(data) {
  return request.post('/parking/book/create', data)
}

// 修改
export function updateParking(data) {
  return request.put('/parking/book/update', data)
}

// 删除（可选）
export function deleteParking(empNo) {
  return request.delete(`/parking/book/delete/${empNo}`)
}


export function saveOrUpdateParking(data) {
    return request.post('/parking/book/saveOrUpdate', data)
}

// 查询预约记录
export function getParkingRecordList(params) {
    return request.get('/parking/record/queryList', { params })
}