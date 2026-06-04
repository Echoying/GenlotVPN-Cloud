-- vpn_role 增加线路字段（对应 line_app.app_id）
ALTER TABLE vpn_role
  ADD COLUMN app_id varchar(64) DEFAULT NULL COMMENT '线路ID(line_app.app_id)' AFTER role_id;

CREATE INDEX idx_vpn_role_app_id ON vpn_role(app_id);
