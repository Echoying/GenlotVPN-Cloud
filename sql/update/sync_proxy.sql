-- 同步代理：line_app 扩展字段（已有库手工执行，列已存在则跳过对应语句）
ALTER TABLE line_app
  ADD COLUMN proxy_host varchar(255) DEFAULT NULL COMMENT '同步代理监听IP，空则用Nacos默认',
  ADD COLUMN proxy_port int(11) DEFAULT NULL COMMENT '同步代理监听端口，空则用Nacos默认';


ALTER TABLE line_app
  ADD COLUMN proxy_enabled char(1) NOT NULL DEFAULT '0' COMMENT '是否启用同步代理 0否直连 1是' AFTER proxy_port;

-- 服务端代理同步 login 固定使用各线路 VPN 用户名为 system 的账号（须已设密码、状态正常）。
-- 可在 Nacos yianlian.sync-proxy.login-username 覆盖默认用户名。
-- sync_proxy 角色仍用于 VPN 桌面端拉取代理配置时的权限校验，与服务端同步 login 无关。
