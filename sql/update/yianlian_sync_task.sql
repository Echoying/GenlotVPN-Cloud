-- 易安联同步补偿任务表
-- 用途：实时 API 代理 login 失败时入队；定时 Job 按 app_id 分批补偿重试
-- 库：ry-cloud
CREATE TABLE IF NOT EXISTS `yianlian_sync_task` (
  `task_id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `biz_type`        VARCHAR(32)  NOT NULL COMMENT '业务类型：DEPT/ROLE/SERVICE/SERVICE_GROUP/VPN_USER/LOCAL_USER/YAL_DEPT_AUTH/YAL_ROLE_AUTH/YAL_USER_AUTH',
  `operation`       VARCHAR(16)  NOT NULL COMMENT '操作：CREATE/UPDATE/DELETE/RESET_PASSWORD/CHANGE_STATUS/ASSIGN_ROLES',
  `app_id`          VARCHAR(64)  NOT NULL COMMENT '线路ID',
  `biz_id`          VARCHAR(64)  DEFAULT NULL COMMENT '本地业务主键（deptId/roleId/userId 等；CREATE 可为空）',
  `payload`         LONGTEXT     NOT NULL COMMENT '同步命令快照（JSON）',
  `status`          CHAR(1)      NOT NULL DEFAULT '0' COMMENT '0待处理 1处理中 2成功 3放弃',
  `retry_count`     INT          NOT NULL DEFAULT 0 COMMENT 'Job 补偿次数',
  `priority`        INT          NOT NULL DEFAULT 100 COMMENT '越小越优先，同线路批次内排序（DEPT<ROLE<USER<AUTH）',
  `last_error`      VARCHAR(500) DEFAULT NULL COMMENT '最近一次错误信息',
  `next_retry_time` DATETIME     DEFAULT NULL COMMENT '下次可执行时间',
  `create_by`       VARCHAR(64)  DEFAULT '' COMMENT '创建者',
  `create_time`     DATETIME     DEFAULT NULL COMMENT '创建时间',
  `update_time`     DATETIME     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`task_id`),
  KEY `idx_status_next` (`status`, `next_retry_time`),
  KEY `idx_app_biz` (`app_id`, `biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='易安联同步补偿任务';

-- 定时补偿 Job（Quartz），如已存在同名任务则跳过；job_id 取当前最大值+1（外层派生表规避 MySQL 1093）
INSERT INTO `sys_job` (`job_id`, `job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`, `remark`)
SELECT (SELECT COALESCE(MAX(job_id), 0) + 1 FROM (SELECT job_id FROM sys_job) t),
       '易安联同步补偿', 'DEFAULT', 'yiAnLianSyncRetryTask.runPending', '0 0/5 * * * ?', '3', '1', '0', 'admin', sysdate(), '每5分钟按线路分批补偿易安联同步失败任务'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM (SELECT invoke_target FROM sys_job) s WHERE s.invoke_target = 'yiAnLianSyncRetryTask.runPending');
