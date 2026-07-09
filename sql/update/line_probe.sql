-- line_app 线路探测字段（已有库执行本脚本）
ALTER TABLE `line_app`
  ADD COLUMN `probe_status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '探测状态 0未探测 1成功 2失败' AFTER `status`,
  ADD COLUMN `probe_time` datetime DEFAULT NULL COMMENT '最近探测时间' AFTER `probe_status`,
  ADD COLUMN `probe_msg` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '探测失败原因' AFTER `probe_time`;

-- 定时任务：VPN线路探测（每分钟，禁止并发）
INSERT INTO `sys_job` (`job_id`, `job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 4, 'VPN线路探测', 'DEFAULT', 'lineProbeTask.run', '0 * * * * ?', '3', '1', '0', 'admin', NOW(), '', NULL, '每分钟探测最多2条启用线路'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_job` WHERE `job_id` = 4);
