-- VPN 在线用户菜单（已有环境执行一次，在 ry-cloud 库执行）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1080, '在线用户', 1061, 8, 'online', 'vpn/online/index', '', '', 1, 0, 'C', '0', '0', 'vpn:online:list', 'online', 'admin', NOW(), 'VPN当前在线用户'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1080);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1081, '在线查询', 1080, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'vpn:online:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1081);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1082, '单条强退', 1080, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'vpn:online:forceLogout', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1082);

-- 为超级管理员角色授权（role_id=1）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.menu_id BETWEEN 1080 AND 1082
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
