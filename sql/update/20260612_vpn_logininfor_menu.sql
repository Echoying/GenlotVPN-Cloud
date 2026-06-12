-- VPN 登录日志菜单（已有环境执行一次）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1068, 'VPN登录日志', 1061, 7, 'logininfor', 'vpn/logininfor/index', '', '', 1, 0, 'C', '0', '0', 'vpn:logininfor:list', 'logininfor', 'admin', NOW(), 'VPN用户登录审计'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1068);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1069, '登录查询', 1068, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'vpn:logininfor:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1069);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1070, '登录删除', 1068, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'vpn:logininfor:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1070);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1071, '日志导出', 1068, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'vpn:logininfor:export', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1071);
