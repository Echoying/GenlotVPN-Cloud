-- vpn_user 用户表
CREATE TABLE `vpn_user` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` varchar(100) NOT NULL COMMENT '用户名',
    `name` varchar(100) DEFAULT NULL COMMENT '姓名',
    `password` varchar(255) DEFAULT NULL COMMENT '密码',
    `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
    `mobile` varchar(20) DEFAULT NULL COMMENT '手机号',
    `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
    `role_id` bigint(20) DEFAULT NULL COMMENT '角色ID',
    `status` char(1) DEFAULT '0' COMMENT '状态(0正常 1停用)',
    `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0代表存在 2代表删除)',
    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `yianlian_id` varchar(100) DEFAULT NULL COMMENT '易安联用户ID',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VPN用户表';
