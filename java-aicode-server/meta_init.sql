-- 清理旧表
DROP TABLE IF EXISTS `meta_entity_file`;
DROP TABLE IF EXISTS `meta_field_definition`;
DROP TABLE IF EXISTS `meta_sequence_tracker`;
DROP TABLE IF EXISTS `meta_ref_file`;
DROP TABLE IF EXISTS `meta_enum_library`;
DROP TABLE IF EXISTS `meta_file_model`;

CREATE TABLE `meta_file_model` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
    `has_header` TINYINT(1) DEFAULT 1 COMMENT '是否有文件头：1-有，0-无',
    `has_footer` TINYINT(1) DEFAULT 1 COMMENT '是否有文件尾：1-有，0-无',
    `split_type` ENUM('DELIMITER', 'FIXED') NOT NULL COMMENT '分隔符或定长',
    `delimiter` VARCHAR(10) DEFAULT NULL COMMENT '分隔符内容',
    `line_ending_char` VARCHAR(10) DEFAULT NULL COMMENT '行结尾固定符号',
    `encoding` VARCHAR(20) DEFAULT 'UTF-8' COMMENT 'UTF-8/GBK，重要：影响长度计算',
    `max_rows_limit` INT DEFAULT 10000 COMMENT '记录数安全阈值-超过则报错',
    `owner_id` VARCHAR(50) NOT NULL COMMENT '创建人ID-记录员工工号',
    `status` ENUM('DRAFT', 'PUBLISHED', 'DISABLED') DEFAULT 'DRAFT' COMMENT '模型状态',
    `model_version` INT DEFAULT 1 COMMENT '模型版本号',
    `shared_with` VARCHAR(100) DEFAULT NULL COMMENT '共享用户ID逗号分隔: 1000001,1000002',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间'
) ENGINE=InnoDB COMMENT='文件模型主表';

CREATE TABLE `meta_field_definition` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `model_id` BIGINT NOT NULL COMMENT '所属模型ID',
    `section` ENUM('FILENAME', 'HEADER', 'BODY', 'FOOTER') NOT NULL COMMENT '字段所属区域：文件名/文件头/正文/文件尾',
    `parent_id` BIGINT DEFAULT NULL COMMENT '仅支持两层，子字段填父ID',
    `field_key` VARCHAR(50) NOT NULL COMMENT '字段唯一变量名，供后面引用',
    `field_name` VARCHAR(100) COMMENT '业务描述',
    `sort_index` INT NOT NULL COMMENT '后端真实排序依据',
    `level` TINYINT DEFAULT 1 COMMENT '字段层级：1-一级，2-二级',
    `length` INT NOT NULL COMMENT '定长模式下的字节/字符长度',
    `is_required` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否必填：1-是，0-否',
    `padding_direction` ENUM('LEFT', 'RIGHT', 'NONE') DEFAULT 'NONE' COMMENT '补位方向：左补位/右补位/不补位',
    `padding_char` VARCHAR(5) DEFAULT ' ' COMMENT '补位字符',
    `rule_type` VARCHAR(30) NOT NULL COMMENT '规则类型: FIXED, DATE, ENUM, REF_FILE, REF_FIELD, SEQ, SUM, COUNT, RANDOM, EXPRESSION',
    -- 冗余关键Key，方便索引和查询引用关系
    `ref_field_key` VARCHAR(50) COMMENT '引用的同报文内字段Key',
    `ref_enum_key` VARCHAR(50) COMMENT '引用的枚举库Key',
    `ref_sequence_key` VARCHAR(50) COMMENT '引用的序列号Key',
    `ref_file_id` BIGINT COMMENT '引用的素材文件ID',
    -- 核心规则配置
    `rule_config_json` JSON NOT NULL COMMENT '包含金额DSL、表达式DSL、随机规则等',
    INDEX `idx_model_sort` (`model_id`, `section`, `sort_index`)
) ENGINE=InnoDB COMMENT='字段定义明细表';

-- 枚举库
CREATE TABLE `meta_enum_library` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `enum_key` VARCHAR(50) UNIQUE NOT NULL COMMENT '枚举唯一标识，如 sex',
    `enum_name` VARCHAR(100) COMMENT '枚举名称/业务描述',
    `items` JSON NOT NULL COMMENT '枚举项JSON: [{"val":"0", "desc":"男"}, {"val":"1", "desc":"女"}]'
) ENGINE=InnoDB COMMENT='枚举库';

-- 引用素材文件
CREATE TABLE `meta_ref_file` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `ref_name` VARCHAR(50) UNIQUE NOT NULL COMMENT '引用名称/唯一标识',
    `file_path` VARCHAR(255) NOT NULL COMMENT '素材文件路径',
    `parse_type` ENUM('DELIMITER', 'FIXED') NOT NULL COMMENT '解析类型：分隔符或定长',
    `delimiter` VARCHAR(10) COMMENT '分隔符内容',
    `column_mapping` JSON COMMENT '列索引与Key映射: {"userName": 1, "age": 2}'
) ENGINE=InnoDB COMMENT='引用素材文件';

-- 序列号与游标追踪
CREATE TABLE `meta_sequence_tracker` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `target_type` ENUM('SEQ', 'REF_FILE') NOT NULL COMMENT '追踪类型：序列号或引用文件',
    `target_id` VARCHAR(100) NOT NULL COMMENT '追踪目标标识: modelId:fieldKey 或 refFileId',
    `current_value` BIGINT DEFAULT 0 COMMENT '当前值/游标位置',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    UNIQUE KEY `uk_target` (`target_type`, `target_id`)
) ENGINE=InnoDB COMMENT='并发锁控与游标表';

-- FTP配置
CREATE TABLE `meta_ftp_config` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '配置名称',
    `ftp_ip` VARCHAR(100) NOT NULL COMMENT 'FTP服务器IP',
    `ftp_port` INT DEFAULT 21 COMMENT 'FTP端口',
    `username` VARCHAR(100) NOT NULL COMMENT 'FTP用户名',
    `password` VARCHAR(200) NOT NULL COMMENT 'FTP密码',
    `remote_path` VARCHAR(500) NOT NULL COMMENT '远程目录路径',
    `create_user` VARCHAR(50) COMMENT '创建人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间'
) ENGINE=InnoDB COMMENT='FTP配置表';

-- 生成记录
CREATE TABLE `meta_entity_file` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `model_id` BIGINT NOT NULL COMMENT '所属模型ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '生成的文件名',
    `file_type` ENUM('PREVIEW', 'FORMAL') NOT NULL COMMENT '文件类型：预览/正式',
    `storage_path` VARCHAR(255) NOT NULL COMMENT '文件存储路径',
    `row_count` INT DEFAULT 0 COMMENT '生成行数',
    `status` ENUM('SUCCESS', 'FAILED', 'RUNNING') DEFAULT 'RUNNING' COMMENT '生成状态：成功/失败/进行中',
    `create_user` VARCHAR(50) COMMENT '创建人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `duration_ms` INT COMMENT '生成耗时(毫秒)',
    `error_msg` TEXT COMMENT '错误信息',
    `temp_path` VARCHAR(255) COMMENT '生成中的临时路径'
) ENGINE=InnoDB COMMENT='生成历史与任务表';
