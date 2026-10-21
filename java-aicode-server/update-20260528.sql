-- ============================================================
-- 聊天房间功能 DDL
-- 执行前请备份数据库
-- ============================================================

-- 1. 创建房间表
CREATE TABLE IF NOT EXISTS chat_room (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL COMMENT '房间标题',
  creator VARCHAR(100) NOT NULL COMMENT '创建者用户名',
  status INT DEFAULT 0 COMMENT '状态：0-正常，1-已销毁（逻辑删除）',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天房间';

-- 2. 创建房间成员表
CREATE TABLE IF NOT EXISTS chat_room_member (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  room_id BIGINT NOT NULL COMMENT '房间ID',
  username VARCHAR(100) NOT NULL COMMENT '用户名',
  joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_room_user (room_id, username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间成员';

-- 3. 消息表增加 room_id 列（现有 chat_message 表需有此列）
-- 如果列已存在会报错，可先检查再执行
ALTER TABLE chat_message ADD COLUMN room_id BIGINT DEFAULT NULL COMMENT '房间ID，NULL表示大厅消息';

-- 4. 创建消息表房间索引
CREATE INDEX idx_room_id ON chat_message(room_id);
