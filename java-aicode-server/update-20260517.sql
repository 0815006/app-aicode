alter TABLE `media_crawl_task` add column `created_by` varchar(7) NOT NULL COMMENT '创建人工号（7位字符串）' after folder_name;

ALTER TABLE `chat_message` 
MODIFY COLUMN `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
MODIFY COLUMN `username` varchar(255) NOT NULL COMMENT '用户名',
MODIFY COLUMN `message` text NOT NULL COMMENT '消息内容',
MODIFY COLUMN `timestamp` datetime NOT NULL COMMENT '消息时间',
COMMENT = '聊天消息表';

ALTER TABLE `chat_file` 
MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
MODIFY COLUMN `file_name` VARCHAR(255) NOT NULL COMMENT '用户看到的原始文件名',
MODIFY COLUMN `storage_name` VARCHAR(255) NOT NULL COMMENT '磁盘上真实的文件名(UUID)',
MODIFY COLUMN `file_size` BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
MODIFY COLUMN `uploader_id` varchar(32) NOT NULL COMMENT '上传者ID-取7位员工工号',
MODIFY COLUMN `uploader_name` VARCHAR(50) COMMENT '上传者昵称（聊天窗口的用户名）',
MODIFY COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
COMMENT = '聊天文件表';

ALTER TABLE `com_workholiday` 
MODIFY COLUMN `date` date NOT NULL COMMENT '日期',
MODIFY COLUMN `status` varchar(255) DEFAULT NULL COMMENT '状态，0 放假 1 上班',
MODIFY COLUMN `msg` varchar(255) DEFAULT NULL COMMENT '节日信息',
COMMENT = '假日信息表';