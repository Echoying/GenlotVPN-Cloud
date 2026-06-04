-- vpn_user 增加线路字段（对应 line_app.app_id）
ALTER TABLE vpn_user
  ADD COLUMN app_id varchar(64) DEFAULT NULL COMMENT '线路ID(line_app.app_id)' AFTER user_id;

CREATE INDEX idx_vpn_user_app_id ON vpn_user(app_id);
