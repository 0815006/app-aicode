-- =============================================================
-- PostgreSQL 支持：扩展数据库连接配置的数据库类型枚举
-- 说明：新增 PostgreSQL，并补齐历史遗漏的 GaussDB
-- 日期：2026-10-13
-- =============================================================

ALTER TABLE `db_connection_config`
  MODIFY COLUMN `db_type` enum('MySQL','TDSQL','GaussDB','PostgreSQL') NOT NULL COMMENT '数据库类型';
