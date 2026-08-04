-- VPN 角色增加选线验证码钉钉群配置（本地字段，不同步易安联）
-- 执行库：ry-cloud
--
-- 联调要点：
-- 1. 在业务库执行本脚本后，重启 ruoyi-modules-yianlian、ruoyi-vpn-auth（以及依赖 api 的相关服务）
-- 2. 管理端「VPN 角色」编辑目标角色，启用「选线验证码钉钉群」并填写机器人 access_token（secret/webhook 可选）
-- 3. 将该角色赋给业务用户对应线路上的 vpn_user；发码应进角色群
-- 4. 未启用/无 token/无角色/Feign 失败 → 仍发到 Nacos dingtalk.robot 默认群

ALTER TABLE `vpn_role`
  ADD COLUMN `dingtalk_enabled` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '是否启用该角色钉钉验证码群（0否 1是）' AFTER `remark`,
  ADD COLUMN `dingtalk_access_token` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '钉钉群机器人 access_token' AFTER `dingtalk_enabled`,
  ADD COLUMN `dingtalk_secret` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '钉钉群机器人加签 secret（可选）' AFTER `dingtalk_access_token`,
  ADD COLUMN `dingtalk_webhook_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '钉钉 webhook 地址（空则用官方默认）' AFTER `dingtalk_secret`;
