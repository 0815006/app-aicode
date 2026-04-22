import request from '@/utils/request'

export function listTasks(params) {
  return request({
    url: '/performance/list',
    method: 'get',
    params
  })
}

export function getTaskDetail(taskId) {
  return request({
    url: '/performance/detail',
    method: 'get',
    params: { taskId }
  })
}

export function recognizeTaskInfo(data) {
  return request({
    url: '/performance/recognize',
    method: 'post',
    data
  })
}

export function saveTask(data) {
  return request({
    url: '/performance/saveTask',
    method: 'post',
    data
  })
}

export function saveTrans(taskId, data) {
  return request({
    url: '/performance/saveTrans',
    method: 'post',
    params: { taskId },
    data
  })
}

export function saveBatches(taskId, data) {
  return request({
    url: '/performance/saveBatches',
    method: 'post',
    params: { taskId },
    data
  })
}

export function saveDatas(taskId, data) {
  return request({
    url: '/performance/saveDatas',
    method: 'post',
    params: { taskId },
    data
  })
}

export function saveScenes(taskId, data) {
  return request({
    url: '/performance/saveScenes',
    method: 'post',
    params: { taskId },
    data
  })
}

export function parseTranExcel(data) {
  return request({
    url: '/performance/tran/parseExcel',
    method: 'post',
    data
  })
}

export function parseBatchExcel(data) {
  return request({
    url: '/performance/tran/parseBatchExcel',
    method: 'post',
    data
  })
}

export function parseDataExcel(data) {
  return request({
    url: '/performance/tran/parseDataExcel',
    method: 'post',
    data
  })
}

export function getDataPlan(taskId) {
  return request({
    url: '/performance/getDataPlan',
    method: 'get',
    params: { taskId }
  })
}

export function saveDataPlan(data) {
  return request({
    url: '/performance/saveDataPlan',
    method: 'post',
    data
  })
}

export function saveDataDetails(taskId, data) {
  return request({
    url: '/performance/saveDataDetails',
    method: 'post',
    params: { taskId },
    data
  })
}

// 模板文件管理
export function listTemplates() {
  return request({
    url: '/performance/file/list',
    method: 'get'
  })
}

export function deleteTemplate(fileName) {
  return request({
    url: '/performance/file/delete',
    method: 'delete',
    params: { fileName }
  })
}

export function getTemplateDownloadUrl(fileName) {
  return `/api/performance/file/download?fileName=${encodeURIComponent(fileName)}`
}

export function getTemplateDownloadByKeywordUrl(keyword) {
  return `/api/performance/file/downloadByKeyword?keyword=${encodeURIComponent(keyword)}`
}
