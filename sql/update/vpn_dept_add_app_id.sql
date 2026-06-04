-- vpn_dept 增加线路字段（对应 line_app.app_id）
ALTER TABLE vpn_dept
  ADD COLUMN app_id varchar(64) DEFAULT NULL COMMENT '线路ID(line_app.app_id)' AFTER dept_id;

CREATE INDEX idx_vpn_dept_app_id ON vpn_dept(app_id);
