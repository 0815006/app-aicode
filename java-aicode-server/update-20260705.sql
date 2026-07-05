-- 车位预约：新增 book_weekdays 字段，支持用户配置仅在指定星期几预约
-- 默认值 1,2,3,4,5 表示周一~周五，向后兼容老用户行为不变
ALTER TABLE parking_book ADD COLUMN book_weekdays VARCHAR(20) DEFAULT '1,2,3,4,5' COMMENT '预约星期，逗号分隔 1=周一 2=周二 3=周三 4=周四 5=周五 6=周六 7=周日';
