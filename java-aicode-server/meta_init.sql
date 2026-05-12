CREATE TABLE `meta_file_model` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
    `has_header` TINYINT(1) DEFAULT 1,
    `has_footer` TINYINT(1) DEFAULT 1,
    `split_type` ENUM('DELIMITER', 'FIXED') NOT NULL COMMENT '分隔符或定长',
    `delimiter` VARCHAR(10) DEFAULT NULL COMMENT '分隔符内容',
    `line_ending_char` VARCHAR(10) DEFAULT NULL COMMENT '行结尾固定符号',
    `encoding` VARCHAR(20) DEFAULT 'UTF-8' COMMENT 'UTF-8/GBK，重要：影响长度计算',
    `max_rows_limit` INT DEFAULT 10000 COMMENT '记录数安全阈值-超过则报错',
    `owner_id` VARCHAR(50) NOT NULL COMMENT '创建人ID-记录员工工号',
    `status` ENUM('DRAFT', 'PUBLISHED', 'DISABLED') DEFAULT 'DRAFT' COMMENT '模型状态',
    `model_version` INT DEFAULT 1 COMMENT '模型版本号',
    `shared_with` VARCHAR(100) DEFAULT NULL COMMENT '存储用户ID逗号分隔: 1000001,1000002',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='文件模型主表';

CREATE TABLE `meta_field_definition` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `model_id` BIGINT NOT NULL,
    `section` ENUM('FILENAME', 'HEADER', 'BODY', 'FOOTER') NOT NULL,
    `parent_id` BIGINT DEFAULT NULL COMMENT '仅支持两层，子字段填父ID',
    `field_key` VARCHAR(50) NOT NULL COMMENT '字段唯一变量名，供后面引用',
    `field_name` VARCHAR(100) COMMENT '业务描述',
    `sort_index` INT NOT NULL COMMENT '后端真实排序依据',
    `level` TINYINT DEFAULT 1 COMMENT '1或2',
    `length` INT NOT NULL COMMENT '定长模式下的字节/字符长度',
    `is_required` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否必填：1-是，0-否',
    `padding_direction` ENUM('LEFT', 'RIGHT', 'NONE') DEFAULT 'NONE',
    `padding_char` VARCHAR(5) DEFAULT ' ',
    `rule_type` VARCHAR(30) NOT NULL COMMENT 'FIXED, DATE, ENUM, REF_FILE, REF_FIELD, SEQ, SUM, COUNT, RANDOM, EXPRESSION',
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
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `enum_key` VARCHAR(50) UNIQUE NOT NULL COMMENT '如 sex',
    `enum_name` VARCHAR(100),
    `items` JSON NOT NULL COMMENT '存储: [{"val":"0", "desc":"男"}, {"val":"1", "desc":"女"}]'
) ENGINE=InnoDB COMMENT='枚举库';

-- 引用素材文件
CREATE TABLE `meta_ref_file` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `ref_name` VARCHAR(50) UNIQUE NOT NULL,
    `file_path` VARCHAR(255) NOT NULL,
    `parse_type` ENUM('DELIMITER', 'FIXED') NOT NULL,
    `delimiter` VARCHAR(10),
    `column_mapping` JSON COMMENT '存储列索引与Key的映射: {"userName": 1, "age": 2}'
) ENGINE=InnoDB COMMENT='引用素材文件';

-- 序列号与游标追踪
CREATE TABLE `meta_sequence_tracker` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `target_type` ENUM('SEQ', 'REF_FILE') NOT NULL,
    `target_id` VARCHAR(100) NOT NULL COMMENT 'modelId:fieldKey 或 refFileId',
    `current_value` BIGINT DEFAULT 0,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_target` (`target_type`, `target_id`)
) ENGINE=InnoDB COMMENT='并发锁控与游标表';

-- 生成记录
CREATE TABLE `meta_entity_file` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `model_id` BIGINT NOT NULL,
    `file_name` VARCHAR(255) NOT NULL,
    `file_type` ENUM('PREVIEW', 'FORMAL') NOT NULL,
    `storage_path` VARCHAR(255) NOT NULL,
    `row_count` INT DEFAULT 0,
    `status` ENUM('SUCCESS', 'FAILED', 'RUNNING') DEFAULT 'RUNNING',
    `create_user` VARCHAR(50),
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `duration_ms` INT COMMENT '耗时',
    `error_msg` TEXT,
    `temp_path` VARCHAR(255) COMMENT '生成中的临时路径'
) ENGINE=InnoDB COMMENT='生成历史与任务表';