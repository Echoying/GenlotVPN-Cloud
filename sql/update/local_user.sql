-- 本地用户与 vpn_user 关联改造（在 ry-cloud 库执行）
-- 可重复执行：已存在列/表时跳过 DDL；数据迁移仅处理 local_user_id IS NULL 的记录

-- 1. 本地用户表
CREATE TABLE IF NOT EXISTS `vpn_local_user` (
  `local_user_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '本地用户ID',
  `user_name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录账号',
  `nick_name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户昵称',
  `email` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '手机号码',
  `sex` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '头像地址',
  `password` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '密码',
  `encrypted_pwd` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'AES加密密码',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `login_ip` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `pwd_update_date` datetime DEFAULT NULL COMMENT '密码最后更新时间',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`local_user_id`),
  UNIQUE KEY `uk_vpn_local_user_name` (`user_name`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='VPN本地用户表';

-- 2. vpn_user 增加 local_user_id（若不存在）
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vpn_user' AND COLUMN_NAME = 'local_user_id'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE vpn_user ADD COLUMN `local_user_id` bigint(20) DEFAULT NULL COMMENT ''来源本地用户ID'' AFTER `app_id`, ADD KEY `idx_vpn_user_local_user_id` (`local_user_id`)',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 历史数据迁移：按 user_name 归并有效线路用户
INSERT INTO vpn_local_user (
  user_name, nick_name, email, phonenumber, sex, avatar, password, encrypted_pwd,
  status, login_ip, login_date, pwd_update_date, remark, create_by, create_time, update_by, update_time, del_flag
)
SELECT t.user_name, t.nick_name, t.email, t.phonenumber, t.sex, t.avatar, t.password, t.encrypted_pwd,
       t.status, t.login_ip, t.login_date, t.pwd_update_date, t.remark, t.create_by, t.create_time, t.update_by, t.update_time, '0'
FROM (
  SELECT u.*,
         @rn := IF(@prev_name = u.user_name, @rn + 1, 1) AS rn,
         @prev_name := u.user_name
  FROM vpn_user u,
       (SELECT @rn := 0, @prev_name := '') vars
  WHERE u.del_flag = '0'
    AND NOT EXISTS (
      SELECT 1 FROM vpn_local_user lu WHERE lu.user_name = u.user_name AND lu.del_flag = '0'
    )
  ORDER BY u.user_name, u.update_time DESC, u.user_id ASC
) t
WHERE t.rn = 1;

UPDATE vpn_user u
INNER JOIN vpn_local_user lu ON lu.user_name = u.user_name AND lu.del_flag = '0'
SET u.local_user_id = lu.local_user_id
WHERE u.del_flag = '0' AND u.local_user_id IS NULL;

-- 4. 同线路防重复同步唯一约束（若不存在）
SET @uk_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vpn_user' AND INDEX_NAME = 'uk_vpn_user_local_app'
);
SET @uk_ddl := IF(@uk_exists = 0,
  'ALTER TABLE vpn_user ADD UNIQUE KEY `uk_vpn_user_local_app` (`local_user_id`,`app_id`)',
  'SELECT 1');
PREPARE stmt2 FROM @uk_ddl;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- 5. 菜单：本地用户 + 线路用户（四字侧栏）
UPDATE sys_menu SET menu_name = '线路用户' WHERE menu_id = 1065;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1072, '本地用户', 1061, 5, 'local/user', 'vpn/local/user/index', NULL, '', 1, 0, 'C', '0', '0', 'vpn:localUser:list', 'user', 'admin', NOW(), '', NULL, 'VPN本地用户管理'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1072);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1073, '本地用户查询', 1072, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'vpn:localUser:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1073);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1074, '本地用户新增', 1072, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'vpn:localUser:add', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1074);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1075, '本地用户修改', 1072, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'vpn:localUser:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1075);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1076, '本地用户删除', 1072, 4, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'vpn:localUser:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1076);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1077, '本地用户重置密码', 1072, 5, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'vpn:localUser:resetPwd', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1077);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1078, '同步到线路', 1072, 6, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'vpn:localUser:sync', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1078);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1079, '离线登录', 1072, 7, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'vpn:localUser:offlineLogin', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1079);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.menu_id BETWEEN 1072 AND 1079
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
