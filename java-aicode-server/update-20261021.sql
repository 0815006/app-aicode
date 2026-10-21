-- =============================================
-- 已下线模块数据库表清理脚本
-- 日期：2026-10-21
-- 范围：性能测试 / 造数引擎 / 资源核查 / 媒体抓取 / 投票功能 五个模块（前后台代码已下线）
-- 说明：现役 Java 代码对以下 21 张表零引用；表间无外键约束，可任意顺序执行
-- 注意：DROP 不可回滚，执行前请对目标库做快照备份；
--       如需保留某模块数据（如 performance_resource_info 迁移底稿），注释掉对应语句即可
-- =============================================

-- ---------- 模块 1：性能测试（原 performance_init.sql，9 张） ----------
DROP TABLE IF EXISTS `perf_task_scene_result_detail`;
DROP TABLE IF EXISTS `perf_task_scene_result`;
DROP TABLE IF EXISTS `perf_task_scene_detail`;
DROP TABLE IF EXISTS `perf_task_scene`;
DROP TABLE IF EXISTS `perf_data_detail`;
DROP TABLE IF EXISTS `perf_data_plan`;
DROP TABLE IF EXISTS `perf_task_batch`;
DROP TABLE IF EXISTS `perf_task_tran`;
DROP TABLE IF EXISTS `perf_task`;

-- ---------- 模块 2：造数引擎（原 meta_init.sql，7 张） ----------
DROP TABLE IF EXISTS `meta_entity_file`;
DROP TABLE IF EXISTS `meta_ftp_config`;
DROP TABLE IF EXISTS `meta_sequence_tracker`;
DROP TABLE IF EXISTS `meta_ref_file`;
DROP TABLE IF EXISTS `meta_enum_library`;
DROP TABLE IF EXISTS `meta_field_definition`;
DROP TABLE IF EXISTS `meta_file_model`;

-- ---------- 模块 3：资源核查（原 init.sql L191，1 张） ----------
-- ⚠️ 业务已迁往 voyage 性能平台，如需留数据做迁移底稿请先备份或注释本条
DROP TABLE IF EXISTS `performance_resource_info`;

-- ---------- 模块 4：媒体抓取（原 init.sql L302，1 张） ----------
-- 注：仅删任务元数据表，磁盘文件 ${DATA_ROOT}/crawl/images|videos 不受影响
DROP TABLE IF EXISTS `media_crawl_task`;

-- ---------- 模块 5：投票功能（原 init.sql L248-288，3 张） ----------
-- 注：投票图片存于磁盘 ${DATA_ROOT}/uploads/vote/，删表不影响已上传文件
DROP TABLE IF EXISTS `vote_records`;
DROP TABLE IF EXISTS `vote_options`;
DROP TABLE IF EXISTS `vote_tasks`;
