/*
SQLyog 企业版 - MySQL GUI v8.14
MySQL - 5.7.17-log : Database - stack_db
*********************************************************************
*/


/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`stack_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 */;

USE `stack_db`;

/*Table structure for table `chat_message` */

DROP TABLE IF EXISTS `chat_message`;

CREATE TABLE `chat_message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `message` text NOT NULL,
  `timestamp` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4;

/*Table structure for table `com_workholiday` */

DROP TABLE IF EXISTS `com_workholiday`;

CREATE TABLE `com_workholiday` (
  `date` date NOT NULL COMMENT '日期',
  `status` varchar(255) DEFAULT NULL COMMENT '状态，0 放假 1 上班',
  `msg` varchar(255) DEFAULT NULL COMMENT '节日信息',
  PRIMARY KEY (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

/*Table structure for table `data_compare_result` */

DROP TABLE IF EXISTS `data_compare_result`;

CREATE TABLE `data_compare_result` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` bigint(20) NOT NULL COMMENT '关联的比对任务ID（逻辑外键，无约束）',
  `table_name` varchar(100) NOT NULL COMMENT '表名',
  `table_comment` varchar(200) DEFAULT NULL COMMENT '表中文名/注释',
  `source_row_count` bigint(20) DEFAULT '0' COMMENT '源库记录数',
  `source_data_size_mb` decimal(10,2) DEFAULT '0.00' COMMENT '源库存储大小（MB）',
  `target_row_count` bigint(20) DEFAULT '0' COMMENT '目标库记录数',
  `target_data_size_mb` decimal(10,2) DEFAULT '0.00' COMMENT '目标库存储大小（MB）',
  `is_structure_match` tinyint(1) DEFAULT NULL COMMENT '结构是否一致',
  `is_row_count_match` tinyint(1) DEFAULT NULL COMMENT '数据量是否一致',
  `is_full_data_match` tinyint(1) DEFAULT NULL COMMENT '完整数据是否一致',
  `structure_diff_detail` json DEFAULT NULL COMMENT '结构差异详情：字段、索引等',
  `row_count_diff_value` bigint(20) DEFAULT NULL COMMENT '数据量差值（正：源多，负：目标多）',
  `full_data_diff_detail` json DEFAULT NULL COMMENT '数据内容差异详情',
  `status` enum('PENDING','COMPARING','DONE','SKIPPED') NOT NULL DEFAULT 'PENDING' COMMENT '执行状态',
  `message` text COMMENT '执行日志或错误信息',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_table_name` (`table_name`),
  KEY `idx_status` (`status`),
  KEY `idx_is_structure_match` (`is_structure_match`),
  KEY `idx_is_row_count_match` (`is_row_count_match`),
  KEY `idx_is_full_data_match` (`is_full_data_match`),
  KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据比对结果明细表（无外键）';

/*Table structure for table `data_compare_task` */

DROP TABLE IF EXISTS `data_compare_task`;

CREATE TABLE `data_compare_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '比对任务ID',
  `task_name` varchar(200) NOT NULL COMMENT '比对任务名称，如：v2.0上线前验证',
  `source_config_id` bigint(20) NOT NULL COMMENT '源库配置ID（冗余，无外键）',
  `source_config_name` varchar(100) NOT NULL COMMENT '源库配置名称快照',
  `source_database` varchar(100) NOT NULL COMMENT '源库名快照',
  `target_config_id` bigint(20) NOT NULL COMMENT '目标库配置ID（冗余，无外键）',
  `target_config_name` varchar(100) NOT NULL COMMENT '目标库配置名称快照',
  `target_database` varchar(100) NOT NULL COMMENT '目标库名快照',
  `compare_type` enum('STRUCTURE','ROW_COUNT','FULL_DATA') NOT NULL COMMENT '比对类型',
  `status` enum('RUNNING','SUCCESS','FAILED','STOPPED') NOT NULL DEFAULT 'RUNNING' COMMENT '任务状态',
  `total_tables` int(11) DEFAULT '0' COMMENT '总表数',
  `matched_tables` int(11) DEFAULT '0' COMMENT '匹配表数',
  `source_only_count` int(11) DEFAULT '0' COMMENT '源库独有表数',
  `target_only_count` int(11) DEFAULT '0' COMMENT '目标库独有表数',
  `structure_diff_count` int(11) DEFAULT '0' COMMENT '结构不一致表数',
  `row_count_diff_count` int(11) DEFAULT '0' COMMENT '数据量不一致表数',
  `full_data_diff_count` int(11) DEFAULT '0' COMMENT '数据内容不一致表数',
  `created_by` varchar(7) NOT NULL COMMENT '创建人工号（7位）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '任务创建时间',
  `finished_time` datetime DEFAULT NULL COMMENT '任务完成时间',
  `duration_seconds` int(11) DEFAULT '0' COMMENT '耗时（秒）',
  `remark` text COMMENT '备注信息',
  PRIMARY KEY (`id`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_created_time` (`created_time`),
  KEY `idx_status` (`status`),
  KEY `idx_compare_type` (`compare_type`),
  KEY `idx_source_db` (`source_database`),
  KEY `idx_target_db` (`target_database`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据比对任务主表（无外键，冗余配置信息）';

/*Table structure for table `db_connection_config` */

DROP TABLE IF EXISTS `db_connection_config`;

CREATE TABLE `db_connection_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_name` varchar(100) NOT NULL COMMENT '配置名称，如：生产库-MySQL',
  `db_type` enum('MySQL','TDSQL') NOT NULL COMMENT '数据库类型',
  `host` varchar(100) NOT NULL COMMENT '主机地址',
  `port` int(11) NOT NULL DEFAULT '3306' COMMENT '端口',
  `username` varchar(50) NOT NULL COMMENT '登录用户名',
  `password` varchar(255) NOT NULL COMMENT '密码（AES-256加密存储）',
  `database_name` varchar(100) NOT NULL COMMENT '数据库名',
  `charset` varchar(20) DEFAULT 'utf8mb4' COMMENT '字符集',
  `created_by` varchar(7) NOT NULL COMMENT '创建人工号（7位字符串）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `status` enum('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  PRIMARY KEY (`id`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_db_type` (`db_type`),
  KEY `idx_status` (`status`),
  KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='数据库连接配置表（无外键）';

/*Table structure for table `parking_book` */

DROP TABLE IF EXISTS `parking_book`;

CREATE TABLE `parking_book` (
  `emp_no` varchar(7) NOT NULL COMMENT '7位工号，用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户中文名称',
  `pass_hash` varchar(255) NOT NULL COMMENT '用户密码哈希（加密存储）',
  `auto_book` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否开启自动预约（0=关闭，1=开启）',
  `next_auto_book_date` date DEFAULT NULL COMMENT '下次自动开启预约日期',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `last_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`emp_no`),
  UNIQUE KEY `uk_emp_no` (`emp_no`) COMMENT '工号唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车位预约配置信息表';

/*Table structure for table `parking_record` */

DROP TABLE IF EXISTS `parking_record`;

CREATE TABLE `parking_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `emp_no` varchar(7) NOT NULL COMMENT '7位工号，用户ID',
  `result` varchar(20) DEFAULT NULL COMMENT '预约结果：成功, 取消',
  `result_desc` varchar(200) DEFAULT NULL COMMENT '预约结果描述',
  `parking_position` varchar(50) DEFAULT NULL COMMENT '车位位置',
  `plate_no` varchar(20) DEFAULT NULL COMMENT '车牌号',
  `parking_type` varchar(10) DEFAULT NULL COMMENT '停车证类型: A, B',
  `appointment_date` date DEFAULT NULL COMMENT '预约入园日期',
  `username` varchar(50) DEFAULT NULL COMMENT '用户姓名',
  `department` varchar(100) DEFAULT NULL COMMENT '用户部门',
  `phone` varchar(20) DEFAULT NULL COMMENT '用户手机号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建日期',
  PRIMARY KEY (`id`),
  KEY `idx_emp_no` (`emp_no`),
  KEY `idx_appointment_date` (`appointment_date`)
) ENGINE=InnoDB AUTO_INCREMENT=98 DEFAULT CHARSET=utf8mb4 COMMENT='预约记录表';

/*Table structure for table `performance_resource_info` */

DROP TABLE IF EXISTS `performance_resource_info`;

CREATE TABLE `performance_resource_info` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `serial_number` int(11) DEFAULT NULL COMMENT '序号',
  `task_name` varchar(255) DEFAULT NULL COMMENT '任务名称',
  `task_num` varchar(32) DEFAULT NULL COMMENT '任务编号',
  `service_name` varchar(255) DEFAULT NULL COMMENT '服务名称',
  `english_short_name` varchar(100) DEFAULT NULL COMMENT '英文简称',
  `batch_name` varchar(32) DEFAULT NULL COMMENT '批次名称',
  `business_dept` varchar(100) DEFAULT NULL COMMENT '业务部门',
  `project_type` varchar(32) DEFAULT NULL COMMENT '项目类型',
  `disaster_backup_level` varchar(32) DEFAULT NULL COMMENT '灾备等级',
  `availability_level` varchar(32) DEFAULT NULL COMMENT '可用性等级',
  `deployment_location` varchar(32) DEFAULT NULL COMMENT '部署地点',
  `network_deployment` varchar(32) DEFAULT NULL COMMENT '网络部署',
  `system_platform` varchar(32) DEFAULT NULL COMMENT '系统平台',
  `paas_platform_type` varchar(32) DEFAULT NULL COMMENT 'PAAS平台类型',
  `theme_count` int(11) DEFAULT NULL COMMENT '主题数量',
  `queue_count` int(11) DEFAULT NULL COMMENT '队列数量',
  `shard_count` int(11) DEFAULT NULL COMMENT '分片数量',
  `per_shard_capacity_gb` int(11) DEFAULT NULL COMMENT '每分片容量（G）',
  `redundancy_method` varchar(32) DEFAULT NULL COMMENT '冗余方式',
  `operating_system` varchar(255) DEFAULT NULL COMMENT '操作系统',
  `middleware` varchar(255) DEFAULT NULL COMMENT '中间件',
  `partition_usage` varchar(100) DEFAULT NULL COMMENT '分区用途',
  `partition_usage_name` varchar(100) DEFAULT NULL COMMENT '分区用途名称',
  `hostname` varchar(32) DEFAULT NULL COMMENT '主机名',
  `ip_address` varchar(32) DEFAULT NULL COMMENT 'IP地址',
  `backup_ip` varchar(32) DEFAULT NULL COMMENT '数据备份IP',
  `cpu_cores` int(11) DEFAULT NULL COMMENT 'CPU核心数',
  `memory_gb` int(11) DEFAULT NULL COMMENT '内存（GB）',
  `dedicated_storage_gb` int(11) DEFAULT NULL COMMENT '独占存储（GB）',
  `shared_storage_id` varchar(32) DEFAULT NULL COMMENT '共享存储编号',
  `san_storage_gb` int(11) DEFAULT NULL COMMENT 'SAN存储（GB）',
  `nas_storage_gb` int(11) DEFAULT NULL COMMENT 'NAS存储（GB）',
  `signature_server` varchar(32) DEFAULT NULL COMMENT '是否有签名服务器',
  `encryption_device` varchar(32) DEFAULT NULL COMMENT '是否有加密机',
  `load_balancer` varchar(32) DEFAULT NULL COMMENT '是否有负载均衡器',
  `ssl_accelerator` varchar(32) DEFAULT NULL COMMENT '是否有SSL加速器',
  `remarks` varchar(2000) DEFAULT NULL COMMENT '备注（外设型号）',
  `partition_role` varchar(100) DEFAULT NULL COMMENT '分区角色',
  `revision_time` datetime DEFAULT NULL COMMENT '修订时间',
  `middleware_reason_below_baseline` varchar(255) DEFAULT NULL COMMENT '中间件使用低于基线版本原因',
  `os_reason_below_baseline` varchar(255) DEFAULT NULL COMMENT '操作系统使用低于基线版本原因',
  `resource_pool` varchar(255) DEFAULT NULL COMMENT '资源池',
  `original_file_name` varchar(255) DEFAULT NULL COMMENT '原始文件名',
  `file_name` varchar(255) DEFAULT NULL COMMENT '上传文件名',
  `product_id` varchar(32) DEFAULT NULL COMMENT '产品标识',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_operator` varchar(32) DEFAULT NULL COMMENT '创建人',
  `last_time` datetime DEFAULT NULL COMMENT '更新时间',
  `last_operator` varchar(32) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=255 DEFAULT CHARSET=utf8mb4 COMMENT='部署方案环境资源清单表';

-- 1. 投票任务表
CREATE TABLE `vote_tasks` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `title` VARCHAR(255) NOT NULL COMMENT '投票任务名称',
    `type` VARCHAR(1) NOT NULL COMMENT '任务类型: "1"-直接投票, "2"-征集投票',
    `max_votes` INT NOT NULL DEFAULT 1 COMMENT '每人总票数限制(永久N票)',
    `allow_view_early` VARCHAR(1) NOT NULL DEFAULT '0' COMMENT '截止前允许查看他人作品: "0"-否, "1"-是',
    `upload_end_at` DATETIME DEFAULT NULL COMMENT '作品上传截止时间(类型2必填)',
    `vote_end_at` DATETIME NOT NULL COMMENT '投票截止时间',
    `creator_id` VARCHAR(7) NOT NULL COMMENT '发起人工号(7位)',
    `status` VARCHAR(1) NOT NULL DEFAULT '0' COMMENT '任务状态: "0"-草稿, "1"-进行中, "2"-已结束',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_creator` (`creator_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投票任务配置表';

-- 2. 投票选项/作品表
CREATE TABLE `vote_options` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `task_id` BIGINT NOT NULL COMMENT '关联任务ID',
    `user_id` VARCHAR(7) NOT NULL COMMENT '贡献者/作者工号(7位)',
    `team_name` VARCHAR(255) COMMENT '队伍名称'，
    `tags` VARCHAR(255) COMMENT '标签',
    `title` VARCHAR(255) NOT NULL COMMENT '作品名称或选项内容',
    `author_name` VARCHAR(100) DEFAULT NULL COMMENT '作者姓名(展示用)',
    `description` TEXT COMMENT '作品介绍',
    `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '作品封面OSS地址',
    `video_url` VARCHAR(500) DEFAULT NULL COMMENT '视频链接或信息',
    `attachment_url` VARCHAR(500) COMMENT '附件地址',
    `audit_status` VARCHAR(1) NOT NULL DEFAULT '0' COMMENT '审核状态: "0"-待审核, "1"-已通过, "2"-驳回',
    `audit_remark` VARCHAR(255) DEFAULT NULL COMMENT '驳回理由',
    `vote_count` INT NOT NULL DEFAULT 0 COMMENT '最终汇总票数(冗余字段用于展示)',
    `last_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_user_task` (`user_id`, `task_id`),
    INDEX `idx_audit` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投票选项作品表';

-- 3. 投票流水记录表
CREATE TABLE `vote_records` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `user_id` VARCHAR(7) NOT NULL COMMENT '投票人工号(7位)',
    `task_id` BIGINT NOT NULL COMMENT '关联任务ID',
    `option_id` BIGINT NOT NULL COMMENT '所投选项ID',
    `is_deleted` VARCHAR(1) NOT NULL DEFAULT '0' COMMENT '改投状态位: "0"-有效, "1"-已撤回/改投',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '投票时间 改投操作时间',
    `last_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '改投操作时间',
    PRIMARY KEY (`id`),
    -- 复合索引：大幅提升判断用户是否已投、查询剩余票数及改投逻辑的性能
    INDEX `idx_user_task_deleted` (`user_id`, `task_id`, `is_deleted`),
    INDEX `idx_option_id` (`option_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投票流水记录表';

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;