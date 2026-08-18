-- VPN 侧栏菜单统一为四字（可重入）
UPDATE sys_menu SET menu_name = '应用分组' WHERE menu_id = 1066 AND menu_name <> '应用分组';
UPDATE sys_menu SET menu_name = '本地用户' WHERE menu_id = 1072 AND menu_name <> '本地用户';
UPDATE sys_menu SET menu_name = '线路用户' WHERE menu_id = 1065 AND menu_name <> '线路用户';
UPDATE sys_menu SET menu_name = '在线用户' WHERE menu_id = 1080 AND menu_name <> '在线用户';
UPDATE sys_menu SET menu_name = '版本策略' WHERE menu_id = 1089 AND menu_name <> '版本策略';
