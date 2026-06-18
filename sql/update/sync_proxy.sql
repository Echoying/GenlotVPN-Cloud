-- 同步代理：line_app 扩展字段（已有库手工执行，列已存在则跳过对应语句）
ALTER TABLE line_app
  ADD COLUMN proxy_host varchar(255) DEFAULT NULL COMMENT '同步代理监听IP，空则用Nacos默认',
  ADD COLUMN proxy_port int(11) DEFAULT NULL COMMENT '同步代理监听端口，空则用Nacos默认';


ALTER TABLE line_app
  ADD COLUMN proxy_enabled char(1) NOT NULL DEFAULT '0' COMMENT '是否启用同步代理 0否直连 1是' AFTER proxy_port;

-- 同步代理管理员角色需按线路手动创建，勿插入全局角色：
-- 1. 在「VPN角色管理」选择对应线路，新增角色：role_key=sync_proxy，名称如「同步代理管理员」
-- 2. 将运维账号绑定该线路的 sync_proxy 角色
-- 若曾执行过全局 sync_proxy 角色（app_id 为空），可手工删除：
-- DELETE FROM vpn_user_role WHERE role_id IN (SELECT role_id FROM vpn_role WHERE role_key='sync_proxy' AND (app_id IS NULL OR app_id=''));
-- DELETE FROM vpn_role WHERE role_key='sync_proxy' AND (app_id IS NULL OR app_id='');
