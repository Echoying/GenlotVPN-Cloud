-- VPN登录日志表
CREATE TABLE `vpn_logininfor` (
  `info_id`     bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `user_name`   varchar(50)  DEFAULT '' COMMENT '用户账号',
  `ipaddr`      varchar(128) DEFAULT '' COMMENT '登录IP地址',
  `status`      char(1)      DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg`         varchar(255) DEFAULT '' COMMENT '提示信息',
  `access_time` datetime     DEFAULT NULL COMMENT '访问时间',
  PRIMARY KEY (`info_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='VPN系统访问记录';
