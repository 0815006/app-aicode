// src/api/migration.js
import request from '@/utils/request'

/**
 * 获取数据库配置列表
 * @returns {Promise} - 返回所有数据库配置的Promise
 */
export function getDbConfigList() {
  return request({
    url: '/db/config/list',
    method: 'get'
  })
}

/**
 * 测试数据库连接
 * @param {Object} config - 数据库连接配置对象
 * @returns {Promise} - 返回连接测试结果的Promise
 */
export function testDbConnection(config) {
  return request({
    url: '/db/config/testConnect',
    method: 'post',
    data: config
  })
}

/**
 * 获取数据库表信息列表
 * @param {Object} config - 数据库配置对象，包含目标库信息
 * @returns {Promise} - 返回表信息的Promise
 */
export function getTableInfoList(config) {
  return request({
    url: '/datamigration/table/list',
    method: 'post',
    data: config
  })
}

/**
 * 创建数据库配置
 * @param {Object} config - 数据库配置对象
 * @returns {Promise} - 返回创建结果的Promise
 */
export function createDbConfig(config) {
  return request({
    url: '/db/config/create',
    method: 'post',
    data: config
  })
}

/**
 * 更新数据库配置
 * @param {Object} config - 数据库配置对象
 * @returns {Promise} - 返回更新结果的Promise
 */
export function updateDbConfig(config) {
  return request({
    url: '/db/config/update',
    method: 'put',
    data: config
  })
}

/**
 * 比对两个数据库的表结构
 * @param {Object} data - 比对所需数据
 * @returns {Promise} - 返回比对结果的Promise
 */
export function compareTableStructure(data) {
  return request({
    url: '/compare/structure',
    method: 'post',
    data
  })
}

/**
 * 比对两个数据库的表记录数
 * @param {Object} data - 比对所需数据
 * @returns {Promise} - 返回记录数比对结果的Promise
 */
export function compareRowCount(data) {
  return request({
    url: '/compare/rowCount',
    method: 'post',
    data
  })
}

/**
 * 比对两个数据库的完整数据
 * @param {Object} data - 比对所需数据
 * @returns {Promise} - 返回完整数据比对结果的Promise
 */
export function compareFullData(data) {
  return request({
    url: '/compare/fullData',
    method: 'post',
    data
  })
}

/**
 * 迁移表结构（从源库到目标库）
 * @param {Object} data - 迁移所需数据
 * @returns {Promise} - 返回迁移结果的Promise
 */
export function migrateTableStructure(data) {
  return request({
    url: '/datamigration/migrate/structure',
    method: 'post',
    data
  })
}

/**
 * 仅迁移表数据（从源库到目标库）
 * @param {Object} data - 迁移所需数据
 * @returns {Promise} - 返回迁移结果的Promise
 */
export function migrateTableDataOnly(data) {
  return request({
    url: '/datamigration/migrate/dataOnly',
    method: 'post',
    data
  })
}

/**
 * 迁移表数据（从源库到目标库）
 * @param {Object} data - 迁移所需数据
 * @returns {Promise} - 返回迁移结果的Promise
 */
export function migrateTableData(data) {
  return request({
    url: '/datamigration/migrate/data',
    method: 'post',
    data
  })
}

/**
 * 清空目标表数据
 * @param {Object} data - 清空所需数据
 * @returns {Promise} - 返回清空结果的Promise
 */
export function truncateTable(data) {
  return request({
    url: '/datamigration/table/truncate',
    method: 'post',
    data
  })
}

/**
 * 删除目标表
 * @param {Object} data - 删除所需数据
 * @returns {Promise} - 返回删除结果的Promise
 */
export function dropTable(data) {
  return request({
    url: '/datamigration/table/drop',
    method: 'post',
    data
  })
}

/**
 * 保存比对任务
 * @param {Object} data - 比对任务数据
 * @returns {Promise} - 返回保存结果的Promise
 */
export function saveCompareTask(data) {
  return request({
    url: '/compare/task/save',
    method: 'post',
    data
  })
}
