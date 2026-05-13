// src/api/meta-gen.js
import request from '@/utils/request'

const BASE = '/meta'

// ===================== 模型管理 =====================

export function listModels(params) {
  return request({ url: `${BASE}/models`, method: 'get', params })
}

export function createModel(data) {
  return request({ url: `${BASE}/models`, method: 'post', data })
}

export function getModelDetail(id) {
  return request({ url: `${BASE}/models/${id}`, method: 'get' })
}

export function updateModel(id, data) {
  return request({ url: `${BASE}/models/${id}`, method: 'put', data })
}

export function deleteModel(id) {
  return request({ url: `${BASE}/models/${id}`, method: 'delete' })
}

export function publishModel(id) {
  return request({ url: `${BASE}/models/${id}/publish`, method: 'post' })
}

export function shareModel(id, data) {
  return request({ url: `${BASE}/models/${id}/share`, method: 'post', data })
}

// ===================== 字段管理 =====================

export function saveFields(modelId, data, section) {
  return request({
    url: `${BASE}/models/${modelId}/fields`,
    method: 'post',
    data,
    params: section ? { section } : undefined
  })
}

export function validateFields(data) {
  return request({ url: `${BASE}/fields/validate`, method: 'post', data })
}

export function getSumTargets(modelId) {
  return request({ url: `${BASE}/fields/sum-targets/${modelId}`, method: 'get' })
}

// ===================== 枚举库管理 =====================

export function listEnums() {
  return request({ url: `${BASE}/enums`, method: 'get' })
}

export function getEnumKeys() {
  return request({ url: `${BASE}/enums/keys`, method: 'get' })
}

export function saveEnum(data) {
  return request({ url: `${BASE}/enums`, method: 'post', data })
}

export function deleteEnum(id) {
  return request({ url: `${BASE}/enums/${id}`, method: 'delete' })
}

// ===================== 引用文件管理 =====================

export function listRefFiles() {
  return request({ url: `${BASE}/resources`, method: 'get' })
}

export function uploadRefFile(formData) {
  return request({
    url: `${BASE}/resources/upload`,
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function defineRefFile(data) {
  return request({ url: `${BASE}/resources/define`, method: 'post', data })
}

export function deleteRefFile(id) {
  return request({ url: `${BASE}/resources/${id}`, method: 'delete' })
}

export function previewRefFile(id, lineCount = 5) {
  return request({ url: `${BASE}/resources/${id}/preview`, method: 'get', params: { lineCount } })
}

// ===================== 模板与导入 =====================

export function listTemplates() {
  return request({ url: `${BASE}/template/list`, method: 'get' })
}

export function deleteTemplate(fileName) {
  return request({ url: `${BASE}/template/delete`, method: 'delete', params: { fileName } })
}

export function getTemplateDownloadUrl(fileName) {
  return `/api/meta/template/download?fileName=${encodeURIComponent(fileName)}`
}

export function getMetaTemplateDownloadByKeywordUrl(keyword) {
  return `/api/meta/template/downloadByKeyword?keyword=${encodeURIComponent(keyword)}`
}

export function getGeneralTemplateDownloadUrl() {
  return `/api/meta/template/general/download`
}

export function exportFieldExcel(data) {
  return request({
    url: `${BASE}/fields/exportExcel`,
    method: 'post',
    data,
    responseType: 'blob'
  })
}

export function parseFieldExcel(formData) {
  return request({
    url: `${BASE}/fields/parseExcel`,
    method: 'post',
    data: formData
  })
}

// ===================== 执行与生成 =====================

export function preview(modelId) {
  return request({ url: `${BASE}/execute/preview/${modelId}`, method: 'post' })
}

export function generate(data) {
  return request({ url: `${BASE}/execute/generate`, method: 'post', data })
}

export function getTaskStatus(taskId) {
  return request({ url: `${BASE}/execute/status/${taskId}`, method: 'get' })
}

export function getHistory(modelId) {
  return request({ url: `${BASE}/execute/history`, method: 'get', params: { modelId } })
}

export function deleteEntityFile(fileId) {
  return request({ url: `${BASE}/execute/file/${fileId}`, method: 'delete' })
}

// ===================== 系统辅助 =====================

export function resetSequence(data) {
  return request({ url: `${BASE}/sys/sequence/reset`, method: 'post', data })
}

export function cleanTemp() {
  return request({ url: `${BASE}/sys/clean-temp`, method: 'post' })
}
