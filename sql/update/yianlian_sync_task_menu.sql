-- 易安联同步任务查询菜单（已有环境执行一次，在 ry-cloud 库执行）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1085, '同步任务', 1061, 10, 'syncTask', 'vpn/syncTask/index', '', '', 1, 0, 'C', '0', '0', 'yianlian:synctask:list', 'job', 'admin', NOW(), '易安联同步补偿任务查询'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1085);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1086, '任务查询', 1085, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'yianlian:synctask:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1086);
