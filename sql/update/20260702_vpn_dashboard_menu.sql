-- VPN 首页仪表盘权限与登录日志统计索引（已有环境执行一次，在 ry-cloud 库执行）

-- 仪表盘查看权限（挂 VPN 管理）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1083, '仪表盘查看', 1061, 9, '#', '', '', '', 1, 0, 'F', '0', '0', 'vpn:dashboard:view', '#', 'admin', NOW(), 'VPN首页仪表盘'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1083);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 1083
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 1083);

-- 登录日志统计索引（可重复执行）
SET @idx_at_status := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vpn_logininfor' AND INDEX_NAME = 'idx_vpn_logininfor_at_status'
);
SET @ddl_at_status := IF(@idx_at_status = 0,
  'ALTER TABLE vpn_logininfor ADD INDEX idx_vpn_logininfor_at_status (access_time, status)',
  'SELECT 1');
PREPARE stmt_at_status FROM @ddl_at_status;
EXECUTE stmt_at_status;
DEALLOCATE PREPARE stmt_at_status;

SET @idx_app_at := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vpn_logininfor' AND INDEX_NAME = 'idx_vpn_logininfor_app_at'
);
SET @ddl_app_at := IF(@idx_app_at = 0,
  'ALTER TABLE vpn_logininfor ADD INDEX idx_vpn_logininfor_app_at (app_id, access_time)',
  'SELECT 1');
PREPARE stmt_app_at FROM @ddl_app_at;
EXECUTE stmt_app_at;
DEALLOCATE PREPARE stmt_app_at;
