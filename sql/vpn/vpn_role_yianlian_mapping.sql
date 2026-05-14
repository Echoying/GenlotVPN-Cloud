-- ----------------------------
-- Table structure for vpn_role_yianlian_mapping
-- ----------------------------
DROP TABLE IF EXISTS `vpn_role_yianlian_mapping`;
CREATE TABLE `vpn_role_yianlian_mapping`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint NOT NULL COMMENT '本地角色ID',
  `app_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '线路应用ID',
  `yianlian_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '易安联角色ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_role_app` (`role_id`, `app_id`) USING BTREE,
  KEY `idx_role_id` (`role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'VPN角色与易安联角色映射表' ROW_FORMAT = DYNAMIC;
