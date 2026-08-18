-- 客户端版本策略（可重入）
CREATE TABLE IF NOT EXISTS vpn_client_version_policy (
  id bigint NOT NULL COMMENT '主键，固定为1',
  enabled char(1) NOT NULL DEFAULT '0' COMMENT '0关 1开',
  min_version varchar(32) DEFAULT '' COMMENT '最低版本 x.y.z',
  download_url_windows varchar(512) DEFAULT '' COMMENT 'Windows下载链接',
  download_url_macos varchar(512) DEFAULT '' COMMENT 'macOS下载链接',
  create_by varchar(64) DEFAULT '' COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT '' COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VPN客户端版本策略';

INSERT INTO vpn_client_version_policy (id, enabled, min_version, download_url_windows, download_url_macos, create_by, create_time, remark)
SELECT 1, '0', '', '', '', 'admin', NOW(), '默认关闭拦截'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM vpn_client_version_policy WHERE id = 1);

-- 菜单 1089 C / 1090 F query / 1091 F edit（1085-1088 已占用）
-- C 菜单 NOT EXISTS 限定 menu_type='C'，避免与 F 菜单同 perms 冲突
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1089, '版本策略', 1061, 11, 'clientVersion', 'vpn/clientVersion/index', NULL, '', 1, 0, 'C', '0', '0', 'vpn:clientVersion:query', 'education', 'admin', NOW(), '最低客户端版本拦截策略'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu
  WHERE menu_id = 1089
     OR (perms = 'vpn:clientVersion:query' AND menu_type = 'C')
);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1090, '版本策略查询', 1089, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'vpn:clientVersion:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1090);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1091, '版本策略修改', 1089, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'vpn:clientVersion:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1091);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.menu_id IN (1089, 1090, 1091)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
