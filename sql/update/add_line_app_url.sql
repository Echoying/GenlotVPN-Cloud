-- 为line_app表添加url字段
ALTER TABLE line_app ADD COLUMN url VARCHAR(255) DEFAULT NULL COMMENT '线路URL' AFTER app_secret;
