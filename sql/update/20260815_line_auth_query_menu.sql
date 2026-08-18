-- 查看线路权限按钮（可重入）
-- 1085/1086 已被同步任务占用，本条用 1087/1088
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1087, '查看线路权限', 1065, 8, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'yianlian:user:queryAuth', '#', 'admin', NOW(), '只读查看用户各线路权限'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1087 OR perms = 'yianlian:user:queryAuth');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1088, '查看线路权限', 1072, 8, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'vpn:localUser:queryAuth', '#', 'admin', NOW(), '只读查看本地用户各线路权限'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1088 OR perms = 'vpn:localUser:queryAuth');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.perms IN ('yianlian:user:queryAuth', 'vpn:localUser:queryAuth')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
