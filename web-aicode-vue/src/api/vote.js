import request from '@/utils/request'

/**
 * 获取投票任务列表
 */
export function getTaskList(params) {
  return request({
    url: '/vote/tasks',
    method: 'get',
    params
  })
}

/**
 * 创建投票任务
 */
export function createTask(data) {
  return request({
    url: '/vote/tasks',
    method: 'post',
    data
  })
}

/**
 * 更新投票任务
 */
export function updateTask(id, data) {
  return request({
    url: `/vote/tasks/${id}`,
    method: 'put',
    data
  })
}

/**
 * 获取任务详情
 */
export function getTaskDetail(id) {
  return request({
    url: `/vote/tasks/${id}`,
    method: 'get'
  })
}

/**
 * 获取单个选项/作品详情
 */
export function getOptionDetail(id) {
  return request({
    url: `/vote/options/${id}`,
    method: 'get'
  })
}

/**
 * 获取选项/作品列表
 */
export function getOptions(taskId) {
  return request({
    url: '/vote/options',
    method: 'get',
    params: { taskId }
  })
}

/**
 * 投票
 */
export function vote(taskId, optionId) {
  return request({
    url: '/vote/vote',
    method: 'post',
    data: { taskId, optionId }
  })
}

/**
 * 撤回投票
 */
export function revokeVote(taskId, optionId) {
  return request({
    url: '/vote/revoke',
    method: 'post',
    data: { taskId, optionId }
  })
}

/**
 * 上传作品
 */
export function uploadWork(data) {
  return request({
    url: '/vote/upload',
    method: 'post',
    data
  })
}

/**
 * 审核作品
 */
export function auditWork(data) {
  return request({
    url: '/vote/audit',
    method: 'post',
    data
  })
}

/**
 * 获取投票结果
 */
export function getResults(taskId) {
  return request({
    url: '/vote/results',
    method: 'get',
    params: { taskId }
  })
}
