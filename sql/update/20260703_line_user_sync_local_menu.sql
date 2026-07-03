-- 线路用户管理：同步本地用户按钮权限（已有环境执行一次，在 ry-cloud 库执行）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1084, '同步本地用户', 1065, 7, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'yianlian:user:syncLocal', '#', 'admin', NOW(), '按部门批量同步本地用户到当前线路'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1084);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 1084
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 1084);
