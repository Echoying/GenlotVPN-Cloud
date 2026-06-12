-- VPN 登录日志：记录线路 ID 与线路名称（客户端上报）
ALTER TABLE `vpn_logininfor`
  ADD COLUMN `app_id` varchar(64) DEFAULT '' COMMENT '线路ID' AFTER `login_purpose`,
  ADD COLUMN `app_name` varchar(64) DEFAULT '' COMMENT '线路名称' AFTER `app_id`;
