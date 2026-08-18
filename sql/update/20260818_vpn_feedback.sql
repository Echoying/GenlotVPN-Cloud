-- VPN 客户端问题反馈（可重入）
CREATE TABLE IF NOT EXISTS vpn_feedback (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  title varchar(80) NOT NULL COMMENT '标题',
  content varchar(2000) NOT NULL COMMENT '描述',
  category varchar(16) DEFAULT '' COMMENT 'connect/login/ui/other',
  status char(1) NOT NULL DEFAULT '0' COMMENT '0待处理 1已排期 2已修复',
  user_id bigint DEFAULT NULL COMMENT 'vpn_user.user_id',
  user_name varchar(64) NOT NULL COMMENT '账号',
  client_version varchar(32) DEFAULT '' COMMENT '客户端版本',
  client_platform varchar(16) DEFAULT '' COMMENT 'windows/macos/unknown',
  ipaddr varchar(128) DEFAULT '' COMMENT '提交IP',
  image_urls varchar(2048) DEFAULT '[]' COMMENT 'JSON数组最多3个url',
  create_by varchar(64) DEFAULT '' COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT '' COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_vpn_feedback_status (status),
  KEY idx_vpn_feedback_ct (create_time),
  KEY idx_vpn_feedback_un (user_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VPN客户端问题反馈';

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '反馈状态', 'vpn_feedback_status', '0', 'admin', NOW(), '问题反馈状态'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'vpn_feedback_status');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '反馈分类', 'vpn_feedback_category', '0', 'admin', NOW(), '问题反馈分类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'vpn_feedback_category');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time)
SELECT 1, '待处理', '0', 'vpn_feedback_status', 'warning', 'Y', '0', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'vpn_feedback_status' AND dict_value = '0');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time)
SELECT 2, '已排期', '1', 'vpn_feedback_status', 'info', 'N', '0', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'vpn_feedback_status' AND dict_value = '1');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time)
SELECT 3, '已修复', '2', 'vpn_feedback_status', 'success', 'N', '0', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'vpn_feedback_status' AND dict_value = '2');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time)
SELECT 1, '连接', 'connect', 'vpn_feedback_category', '', 'N', '0', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'vpn_feedback_category' AND dict_value = 'connect');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time)
SELECT 2, '登录', 'login', 'vpn_feedback_category', '', 'N', '0', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'vpn_feedback_category' AND dict_value = 'login');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time)
SELECT 3, '界面', 'ui', 'vpn_feedback_category', '', 'N', '0', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'vpn_feedback_category' AND dict_value = 'ui');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time)
SELECT 4, '其他', 'other', 'vpn_feedback_category', '', 'N', '0', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'vpn_feedback_category' AND dict_value = 'other');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1092, '问题反馈', 1061, 12, 'feedback', 'vpn/feedback/index', NULL, '', 1, 0, 'C', '0', '0', 'vpn:feedback:list', 'message', 'admin', NOW(), 'VPN客户端问题反馈'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu
  WHERE menu_id = 1092
     OR (perms = 'vpn:feedback:list' AND menu_type = 'C')
);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1093, '反馈查询', 1092, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'vpn:feedback:list', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1093);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1094, '反馈详情', 1092, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'vpn:feedback:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1094);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 1095, '反馈改状态', 1092, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'vpn:feedback:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1095);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.menu_id IN (1092, 1093, 1094, 1095)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);

INSERT INTO vpn_feedback (title, content, category, status, user_name, image_urls, create_by, create_time)
SELECT '验收用反馈', '流水线种子数据', '', '0', 'accept', '[]', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM vpn_feedback WHERE title = '验收用反馈' AND user_name = 'accept');
