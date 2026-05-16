// src/api/media-crawl.js
import request from '@/utils/request'

/**
 * 添加抓取任务
 */
export function addCrawlTask(url, crawlType, minSizeLimit, createdBy) {
  return request({
    url: '/media-crawl/task/add',
    method: 'post',
    params: { url, crawlType, minSizeLimit, createdBy }
  })
}

/**
 * 分页查询任务列表（仅当前用户）
 */
export function listCrawlTasks(page, size, createdBy) {
  return request({
    url: '/media-crawl/task/list',
    method: 'get',
    params: { page, size, createdBy }
  })
}

/**
 * 获取任务详情
 */
export function getCrawlTask(id) {
  return request({
    url: `/media-crawl/task/${id}`,
    method: 'get'
  })
}

/**
 * 获取任务对应的本地媒体文件列表
 */
export function getTaskMediaFiles(id) {
  return request({
    url: `/media-crawl/task/${id}/media-files`,
    method: 'get'
  })
}

/**
 * 手动触发抓取引擎
 */
export function startCrawlEngine() {
  return request({
    url: '/media-crawl/engine/start',
    method: 'post'
  })
}

/**
 * 删除任务（连带删除本地文件）
 */
export function deleteCrawlTask(id) {
  return request({
    url: `/media-crawl/task/${id}`,
    method: 'delete'
  })
}

/**
 * 删除指定媒体文件（物理删除）
 */
export function deleteMediaFiles(folderName, type, fileNames) {
  return request({
    url: '/media-crawl/media-files',
    method: 'delete',
    params: { folderName, type, fileNames: fileNames.join(',') }
  })
}
