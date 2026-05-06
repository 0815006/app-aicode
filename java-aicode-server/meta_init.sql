CREATE TABLE `meta_file_definition` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '定义名称',
  `file_name_rule` varchar(200) NOT NULL COMMENT '文件名生成规则（如PAY_{yyyyMMdd}.txt）',
  `encoding` varchar(20) DEFAULT 'UTF-8' COMMENT '字符集（GBK/UTF-8）',
  `line_separator` varchar(10) DEFAULT 'CRLF' COMMENT '换行符（CRLF/LF）',
  `parse_mode` varchar(20) DEFAULT 'FIXED' COMMENT '模式（FIXED/DELIMITER）',
  `delimiter` varchar(10) DEFAULT NULL COMMENT '分隔符（分隔符模式用）',
  `header_rows` int(11) DEFAULT '1' COMMENT '文件头行数',
  `footer_rows` int(11) DEFAULT '0' COMMENT '文件尾行数',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='元数据-文件定义表';

CREATE TABLE `meta_field_definition` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_id` bigint(20) NOT NULL COMMENT '关联文件定义ID',
  `section_type` varchar(20) NOT NULL COMMENT '区块类型（HEADER/BODY/FOOTER）',
  `field_name` varchar(100) NOT NULL COMMENT '字段名称',
  `start_pos` int(11) DEFAULT NULL COMMENT '起止位置-起始（定长模式用）',
  `end_pos` int(11) DEFAULT NULL COMMENT '起止位置-结束（定长模式用）',
  `field_index` int(11) DEFAULT NULL COMMENT '字段索引（分隔符模式用）',
  `data_type` varchar(20) DEFAULT 'String' COMMENT '类型（String/Number/Amount/Enum）',
  `gen_rule_config` json NOT NULL COMMENT '规则配置JSON（含默认值、填充规则、资源池引用、汇总算法等）',
  `sort_order` int(11) DEFAULT '0' COMMENT '排序序号',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_file_id` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='元数据-字段规则表';


CREATE TABLE `meta_resource_pool` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `pool_name` varchar(100) NOT NULL COMMENT '资源池名称',
  `file_path` varchar(255) NOT NULL COMMENT '服务器本地存储路径',
  `delimiter` varchar(10) DEFAULT ',' COMMENT '资源文件列分隔符',
  `column_mapping` json DEFAULT NULL COMMENT '列名映射（如{"0":"账号","1":"户名"}）',
  `current_index` int(11) DEFAULT '0' COMMENT '顺序循环取值时的当前行指针',
  `total_rows` int(11) DEFAULT '0' COMMENT '总记录数',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='元数据-资源池表';

CREATE TABLE `meta_generation_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_id` bigint(20) NOT NULL COMMENT '关联文件定义ID',
  `file_name` varchar(200) NOT NULL COMMENT '最终生成的文件名',
  `local_path` varchar(255) NOT NULL COMMENT '本地存放路径',
  `file_size` bigint(20) DEFAULT '0' COMMENT '文件大小（字节）',
  `row_count` int(11) DEFAULT '0' COMMENT '生成的Body行数',
  `creator` varchar(50) DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_file_id` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='元数据-生成历史表';
