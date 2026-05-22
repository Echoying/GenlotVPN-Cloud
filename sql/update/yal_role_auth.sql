CREATE TABLE `yal_role_auth` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `line_id` varchar(100) NOT NULL COMMENT '线路appId',
  `app_group_ids` varchar(2000) DEFAULT NULL COMMENT '应用组ID列表，逗号分隔',
  `app_ids` varchar(2000) DEFAULT NULL COMMENT '应用ID列表，逗号分隔',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色授权表';
