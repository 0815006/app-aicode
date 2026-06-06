-- ============================================
-- parking_book 表增量更新 - 邮件通知功能
-- 日期: 2026-06-06
-- 说明: 为 parking_book 表新增邮件通知相关字段
-- ============================================

USE `stack_db`;

ALTER TABLE `parking_book`
    ADD COLUMN `email_enabled` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否开启邮件通知（0=关闭，1=开启）' AFTER `next_auto_book_date`,
    ADD COLUMN `email_user` varchar(50) DEFAULT NULL COMMENT '发送邮件账号用户名（7位工号格式）' AFTER `email_enabled`,
    ADD COLUMN `email_password` varchar(255) DEFAULT NULL COMMENT '发送邮件账号密码（加密存储）' AFTER `email_user`,
    ADD COLUMN `email_recipient` varchar(255) DEFAULT NULL COMMENT '收件人邮箱地址' AFTER `email_password`;
