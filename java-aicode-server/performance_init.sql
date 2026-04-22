-- 性能测试任务主表
CREATE TABLE IF NOT EXISTS `perf_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `product_id` varchar(64) NOT NULL COMMENT '产品/牵头组件标识 (如：BPS-D-AUTO)',
  `batch_no` varchar(32) NOT NULL COMMENT '批次 (如：2606)',
  `task_name` varchar(255) DEFAULT NULL COMMENT '任务名称',
  `test_task_no` varchar(64) DEFAULT NULL COMMENT '测试任务编号',
  `prod_task_no` varchar(64) DEFAULT NULL COMMENT '生产任务编号',
  `req_no` varchar(64) DEFAULT NULL COMMENT '需求编号',
  `proj_name` varchar(255) DEFAULT NULL COMMENT '项目名称',
  `proj_no` varchar(64) DEFAULT NULL COMMENT '项目编号',
  `test_dept` varchar(128) DEFAULT NULL COMMENT '测试部门',
  `dev_dept` varchar(128) DEFAULT NULL COMMENT '开发部门',
  `start_time` datetime DEFAULT NULL COMMENT '测试开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '测试结束时间',
  `perf_manager` varchar(64) DEFAULT NULL COMMENT '性能测试经理姓名',
  `test_arch` varchar(64) DEFAULT NULL COMMENT '测试架构师姓名',
  `project_manager` varchar(64) DEFAULT NULL COMMENT '项目经理姓名',
  `recorder_range` text DEFAULT NULL COMMENT '填报人员范围 (7位员工号，逗号分隔)',
  `creator_id` varchar(7) NOT NULL COMMENT '创建人ID (7位员工号)',
  `status` int(11) DEFAULT '10' COMMENT '任务状态 (10:新建/调研中, 20:方案待明确, 30:已定稿)',
  `total_user_count` decimal(12,4) DEFAULT NULL COMMENT '用户数总数',
  `daily_online_user_count` decimal(12,4) DEFAULT NULL COMMENT '日均在线用户数',
  `daily_peak_tps` decimal(10,3) DEFAULT NULL COMMENT '日交易峰值TPS',
  `annual_peak_tps` decimal(10,3) DEFAULT NULL COMMENT '年交易峰值TPS',
  `selected_tran_tps_sum` decimal(10,3) DEFAULT NULL COMMENT '选中交易TPS之和',
  `batch_total_duration` varchar(64) DEFAULT NULL COMMENT '预估整体批量时长',
  `batch_total_data_volume` varchar(128) DEFAULT NULL COMMENT '预估整体数据量',
  `batch_parallel_degree` varchar(64) DEFAULT NULL COMMENT '并行度',
  `batch_max_parallel_count` varchar(32) DEFAULT NULL COMMENT '最大并行数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `last_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_batch_product` (`batch_no`, `product_id`),
  KEY `idx_creator` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='性能测试任务主表';

-- 联机交易调研与指标方案表
CREATE TABLE `perf_task_tran` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint(20) NOT NULL COMMENT '关联主表perf_task的id',
  -- 基础信息
  `module_name` varchar(64) DEFAULT NULL COMMENT '产品名称/模块名称*',
  `tran_name` varchar(128) NOT NULL COMMENT '交易名称*',
  `tran_code` varchar(128) DEFAULT NULL COMMENT '交易代码/apicode',
  `interface_type` varchar(64) DEFAULT NULL COMMENT '用户群说明/接口类型*',
  -- 1. 生产现状调研情况 (tran_)
  `tran_daily_vol` decimal(12,4) DEFAULT NULL COMMENT '生产日峰值交易量(万笔/日)*',
  `tran_peak_hour_vol` decimal(12,4) DEFAULT NULL COMMENT '生产高峰时段量(万笔/小时)*',
  `tran_peak_tps` decimal(10,3) DEFAULT NULL COMMENT '生产每秒最大TPS*',
  `tran_avg_rt` decimal(10,3) DEFAULT NULL COMMENT '生产平均响应时间(秒)*',
  `tran_max_rt` decimal(10,3) DEFAULT NULL COMMENT '生产最大响应时间(秒)',
  -- 2. 业务交易指标明细要求 (target_)
  `target_daily_vol` decimal(12,4) DEFAULT NULL COMMENT '目标日峰值量(万笔/日)*',
  `target_peak_hour_vol` decimal(12,4) DEFAULT NULL COMMENT '目标高峰时段量(万笔/小时)*',
  `target_tps` decimal(10,3) DEFAULT NULL COMMENT '测试目标TPS*',
  `target_rt` decimal(10,3) DEFAULT NULL COMMENT '目标平均响应时间(秒)*',
  `target_max_rt` decimal(10,3) DEFAULT NULL COMMENT '目标最大响应时间(秒)*',
  `target_success_rate` decimal(5,2) DEFAULT '100.00' COMMENT '目标交易成功率(%)*',
  `target_think_time` decimal(10,3) DEFAULT NULL COMMENT '用户平均操作时间/思考时间(秒)',
  -- 3. 交易选择结果 (直接体现复杂度与选取逻辑)
  `is_selected` tinyint(1) DEFAULT '1' COMMENT '是否选为性能测试交易*',
  `select_reason` varchar(512) DEFAULT NULL COMMENT '选取原因(多选枚举，含复杂度说明)*',
  -- 4. 指标推算审计
  `indicator_source` int(11) DEFAULT '1' COMMENT '指标来源(1:实测, 2:采样折算, 3:经验对标)',
  `calculation_process` text DEFAULT NULL COMMENT '指标推算过程（针对采样折算或经验对标的详细描述）',
  -- 系统字段
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联机交易调研与指标方案表';

-- 批量作业调研与指标方案表
CREATE TABLE `perf_task_batch` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint(20) NOT NULL COMMENT '关联主表perf_task的id',
  -- 基础属性 (对应Excel前段部分)
  `job_no` varchar(32) DEFAULT NULL COMMENT '作业编号',
  `job_name` varchar(128) NOT NULL COMMENT '批量作业名称',
  `job_count` varchar(32) DEFAULT '1' COMMENT '批量作业数',
  `job_parallel_mode` varchar(64) DEFAULT NULL COMMENT '并行方式',
  `job_desc` text COMMENT '批量作业功能简述',
  `job_trigger_cond` varchar(255) DEFAULT NULL COMMENT '触发条件',
  `job_pre_name` varchar(255) DEFAULT NULL COMMENT '前导作业名',
  `job_concurrent_names` text DEFAULT NULL COMMENT '可同时并行作业名',
  `job_frequency` varchar(32) DEFAULT NULL COMMENT '执行频率',
  -- 生产调研指标 (增加 job_actual_duration 字段)
  `job_data_type` varchar(64) DEFAULT NULL COMMENT '最大数据量级数据类型',
  `job_data_volume` varchar(128) DEFAULT NULL COMMENT '预估数据量级',
  `job_actual_duration` varchar(64) DEFAULT NULL COMMENT '生产调研实际运行时长(如:25min)',
  `job_duration` varchar(64) DEFAULT NULL COMMENT '预估处理时长/性能要求时长(如:20min)', 
  `job_exec_time_point` varchar(255) DEFAULT NULL COMMENT '生产上批量执行时间点',
  -- 方案与机制 (对应Excel后段部分)
  `is_mixed_link` varchar(16) DEFAULT '否' COMMENT '是否叠加联机交易',
  `mixed_tran_names` varchar(255) DEFAULT NULL COMMENT '叠加联机交易名称',
  `has_retry` varchar(64) DEFAULT '是' COMMENT '是否有重做机制',
  `retry_desc` text DEFAULT NULL COMMENT '重做机制简述',
  -- 交易选择结果 (直接体现复杂度与选取逻辑)
  `select_reason` varchar(512) DEFAULT NULL COMMENT '选取原因(多选枚举，参考行业经验定义)',
  -- 审计字段
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批量作业调研与指标方案表';

-- 性能测试数据准备方案主表(定性描述)
CREATE TABLE IF NOT EXISTS `perf_data_plan` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint(20) NOT NULL COMMENT '关联性能任务ID',
  -- 二、数据模型分析部分
  `model_analysis` text COMMENT '相关数据内容分析(2.1章节)',
  `data_constraint` text COMMENT '数据约束说明(2.4章节)',
  -- 三、数据构造设计部分
  `data_source_desc` text COMMENT '数据来源说明(3.1章节)',
  `prep_method_desc` text COMMENT '数据构造/准备方法描述(3.2章节)',
  `cleaning_rule` text COMMENT '数据脱敏/清洗规则(3.3章节)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='性能测试数据准备方案主表(定性描述)';

-- 性能测试数据准备明细表(表级规模)
CREATE TABLE IF NOT EXISTS `perf_data_detail` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `task_id` bigint(20) NOT NULL,
  `data_type` int(11) DEFAULT '1' COMMENT '数据分类(1:核心业务表, 2:基础数据/码表)',
  -- 对应Word表格与Excel导入列
  `table_name_en` varchar(128) NOT NULL COMMENT '英文表名*',
  `table_name_cn` varchar(128) DEFAULT NULL COMMENT '中文表名/描述*',
  -- 生产存量指标
  `table_rows_count` bigint(20) DEFAULT NULL COMMENT '生产当前存量行数(万行)',
  `table_growth_rate` decimal(5,2) DEFAULT NULL COMMENT '预估年增长率(%)',
  -- 方案目标指标
  `target_rows_count` bigint(20) DEFAULT NULL COMMENT '测试目标造数行数(万行)*',
  `data_dist_desc` text DEFAULT NULL COMMENT '数据量情况/数据特征分布(2.3章节列)',
  -- 准备方式
  `prep_method` varchar(64) DEFAULT '脚本自造' COMMENT '准备方式(生产脱敏借数/脚本自造)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='性能测试数据准备明细表(表级规模)';

-- 性能测试场景定义与实测结果表
CREATE TABLE `perf_task_scene` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint(20) NOT NULL COMMENT '关联主表id',
  `scene_type` int(11) NOT NULL COMMENT '1:基准, 2:单负载, 3:混合负载, 4:稳定性, 5:专项/极限, 6:批量',
  `scene_name` varchar(128) NOT NULL COMMENT '场景名称',
  -- 方案阶段维度
  `is_selected` tinyint(1) DEFAULT '1' COMMENT '方案阶段是否勾选(1:是, 0:否)',
  `select_reason` varchar(255) DEFAULT NULL COMMENT '勾选原因或专项场景说明',
  -- 指标要求 (指标由调研表推算而来)
  `target_tps` decimal(10,3) DEFAULT NULL COMMENT '预期TPS',
  `target_rt` decimal(10,3) DEFAULT NULL COMMENT '预期响应时间(秒)',
  `target_success_rate` decimal(5,2) DEFAULT '100.00' COMMENT '预期成功率(%)',
  `test_setting_json` text COMMENT '压测配置(并发数/Ramp-Up/持续时间等)',
  -- 执行结果 (方案阶段为空，测试完成后填报)
  `actual_tps` decimal(10,3) DEFAULT NULL COMMENT '实测TPS',
  `actual_rt` decimal(10,3) DEFAULT NULL COMMENT '实测平均RT(秒)',
  `actual_rt_90` decimal(10,3) DEFAULT NULL COMMENT '实测90%RT(秒)',
  `actual_success_rate` decimal(5,2) DEFAULT NULL COMMENT '实测成功率(%)',
  -- 时间审计 (核心需求：用于获取监控图)
  `run_start_time` datetime DEFAULT NULL COMMENT '场景实际执行开始时间',
  `run_end_time` datetime DEFAULT NULL COMMENT '场景实际执行结束时间',
  -- 审计字段
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='性能测试场景定义与实测结果表';
