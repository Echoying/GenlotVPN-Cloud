package com.ruoyi.yianlian.domain;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 易安联同步补偿任务 yianlian_sync_task
 *
 * <p>实时 API 代理 login 失败时入队；定时 Job 按 app_id 分批补偿重试。</p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class YianlianSyncTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 状态：待处理 */
    public static final String STATUS_PENDING = "0";
    /** 状态：处理中 */
    public static final String STATUS_PROCESSING = "1";
    /** 状态：成功 */
    public static final String STATUS_SUCCESS = "2";
    /** 状态：放弃 */
    public static final String STATUS_ABANDONED = "3";

    /** 任务ID */
    private Long taskId;

    /** 业务类型 */
    private String bizType;

    /** 操作类型 */
    private String operation;

    /** 线路ID */
    private String appId;

    /** 本地业务主键（CREATE 可为空） */
    private String bizId;

    /** 同步命令快照（JSON） */
    private String payload;

    /** 状态：0待处理 1处理中 2成功 3放弃 */
    private String status;

    /** Job 补偿次数 */
    private Integer retryCount;

    /** 优先级，越小越优先 */
    private Integer priority;

    /** 最近一次错误信息 */
    private String lastError;

    /** 下次可执行时间 */
    private Date nextRetryTime;

    /** 查询用：多线路筛选（逗号分隔，非表字段） */
    private String appIds;

    /** 列表展示：业务对象名称（非表字段） */
    private String bizName;
}
