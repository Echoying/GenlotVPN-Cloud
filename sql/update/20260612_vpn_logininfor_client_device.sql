-- vpn_logininfor 增加客户端设备信息
ALTER TABLE vpn_logininfor
  ADD COLUMN client_os varchar(128) DEFAULT '' COMMENT '客户端操作系统' AFTER ipaddr,
  ADD COLUMN client_mac varchar(32) DEFAULT '' COMMENT '客户端MAC地址' AFTER client_os;
