package com.ruoyi.yianlian.service.sync.orchestrator;

/**
 * 同步已延迟：实时 API 因代理会话不可用（login 失败/锁繁忙）已写入补偿队列，本地未变更。
 * 上层应据此返回「已加入补偿队列」（HTTP 202 语义）。
 */
public class SyncDeferredException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    /** 补偿任务ID */
    private final Long taskId;

    public SyncDeferredException(Long taskId, String message)
    {
        super(message);
        this.taskId = taskId;
    }

    public Long getTaskId()
    {
        return taskId;
    }
}
