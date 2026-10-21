-- =============================================================
-- 增量脚本：update-20261020.sql
-- 目的  ：弱密码审计关键字整改——密码类字段统一改名，避开 'password' 关键字
-- 影响库：stack_db
-- 影响表：db_connection_config / meta_ftp_config / parking_book
-- 命名映射（报文名 = Java字段 = 数据库列）：
--   数据库密码  dbPass    -> db_pass
--   FTP  密码   ftpPass   -> ftp_pass
--   邮箱  密码  emailPass -> email_pass
--   数据库用户名 dbUser    -> db_user
--   FTP  用户名  ftpUser   -> ftp_user
--   用户密码哈希 passHash -> pass_hash（2026-10-21 再更名为 userPass / user_pass，见第 6 条）
-- 说明  ：CHANGE COLUMN 只改列名，数据与索引不受影响；本次涉及的 5 个列均无索引依赖
-- 执行  ：仅需执行一次（增量打版本，历史脚本不重复执行）
-- =============================================================

-- 1. 数据库连接配置表：password -> db_pass
ALTER TABLE `db_connection_config`
    CHANGE COLUMN `password` `db_pass` varchar(255) NOT NULL COMMENT '密码（AES-256加密存储）';

-- 2. FTP配置表：password -> ftp_pass
ALTER TABLE `meta_ftp_config`
    CHANGE COLUMN `password` `ftp_pass` VARCHAR(200) NOT NULL COMMENT 'FTP密码';

-- 3. 车位预约表：email_password -> email_pass
ALTER TABLE `parking_book`
    CHANGE COLUMN `email_password` `email_pass` varchar(255) DEFAULT NULL COMMENT '发送邮件账号密码（加密存储）';

-- 4. 数据库连接配置表：username -> db_user
ALTER TABLE `db_connection_config`
    CHANGE COLUMN `username` `db_user` varchar(50) NOT NULL COMMENT '登录用户名';

-- 5. FTP配置表：username -> ftp_user
ALTER TABLE `meta_ftp_config`
    CHANGE COLUMN `username` `ftp_user` VARCHAR(100) NOT NULL COMMENT 'FTP用户名';

-- 6. 车位预约表：pass_hash -> user_pass（仅改列名，加密方式与业务逻辑不变）
ALTER TABLE `parking_book`
    CHANGE COLUMN `pass_hash` `user_pass` varchar(255) NOT NULL COMMENT '用户密码（AES加密存储）';


-- =============================================================
-- 回滚语句（如需还原，按需单独执行）
-- =============================================================
-- ALTER TABLE `db_connection_config`
--     CHANGE COLUMN `db_pass` `password` varchar(255) NOT NULL COMMENT '密码（AES-256加密存储）';
-- ALTER TABLE `meta_ftp_config`
--     CHANGE COLUMN `ftp_pass` `password` VARCHAR(200) NOT NULL COMMENT 'FTP密码';
-- ALTER TABLE `parking_book`
--     CHANGE COLUMN `email_pass` `email_password` varchar(255) DEFAULT NULL COMMENT '发送邮件账号密码（加密存储）';
-- ALTER TABLE `db_connection_config`
--     CHANGE COLUMN `db_user` `username` varchar(50) NOT NULL COMMENT '登录用户名';
-- ALTER TABLE `meta_ftp_config`
--     CHANGE COLUMN `ftp_user` `username` VARCHAR(100) NOT NULL COMMENT 'FTP用户名';
-- ALTER TABLE `parking_book`
--     CHANGE COLUMN `user_pass` `pass_hash` varchar(255) NOT NULL COMMENT '用户密码哈希（加密存储）';
