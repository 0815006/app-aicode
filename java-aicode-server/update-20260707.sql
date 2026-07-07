-- update-20260707.sql
-- 文件共享功能

CREATE TABLE IF NOT EXISTS `shared_file` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
    `file_size` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    `sub_directory_name` VARCHAR(100) NOT NULL COMMENT '所属子目录名称',
    `physical_path` VARCHAR(500) NOT NULL COMMENT '物理存储路径',
    `uploader_id` VARCHAR(7) NOT NULL COMMENT '上传用户7位工号',
    `uploader_ip` VARCHAR(50) DEFAULT NULL COMMENT '上传人的IP',
    `download_count` INT(11) NOT NULL DEFAULT 0 COMMENT '下载次数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_sub_directory` (`sub_directory_name`),
    INDEX `idx_uploader_id` (`uploader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件共享表';
