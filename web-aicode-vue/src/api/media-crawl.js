// src/api/media-crawl.js
import request from '@/utils/request'

/**
 * 添加抓取任务
 */
export function addCrawlTask(url, crawlType, minSizeLimit) {
  return request({
    url: '/media-crawl/task/add',
    method: 'post',
    params: { url, crawlType, minSizeLimit }
  })
}

/**
 * 分页查询任务列表
 */
export function listCrawlTasks(page, size) {
  return request({
    url: '/media-crawl/task/list',
    method: 'get',
    params: { page, size }
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
