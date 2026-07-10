package com.ruoyi.yianlian.service.sync.task;

import com.ruoyi.yianlian.domain.YianlianSyncTask;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncCommand;

import java.util.List;

/**
 * 易安联同步补偿任务服务
 */
public interface IYiAnLianSyncTaskService
{
    /**
     * 入队补偿任务（同 bizType+operation+appId+bizId 待处理任务存在则更新）
     *
     * @param command  同步命令
     * @param error    失败原因
     * @param priority 优先级
     * @return 任务ID
     */
    Long enqueue(SyncCommand command, String error, int priority);

    /**
     * 查询到期可执行的 appId 列表（去重、按优先级）
     */
    List<String> listDueAppIds(int limit);

    /**
     * 查询指定线路到期可执行任务（按优先级、task_id）
     */
    List<YianlianSyncTask> listDueTasksByAppId(String appId, int limit);

    /**
     * 原子抢占指定线路的到期任务（status 0 → 1）
     */
    List<YianlianSyncTask> claimDueTasksByAppId(String appId, int limit);

    /**
     * 回收超时仍在处理中的任务
     *
     * @return 回收条数
     */
    int resetStaleProcessing(int staleMinutes);

    /**
     * 标记成功
     */
    void markSuccess(YianlianSyncTask task);

    /**
     * 记录一次失败：retry_count++，达上限置放弃，否则延后重试
     */
    void markFailure(YianlianSyncTask task, String error, int maxRetries);

    /**
     * 永久失败：立即放弃，不再重试
     */
    void markAbandoned(YianlianSyncTask task, String error);

    /**
     * 批次 login 失败：该线路本批任务集体延后（retry_count++）
     */
    void deferBatch(List<YianlianSyncTask> tasks, String error, int maxRetries);

    /**
     * 反序列化任务为同步命令
     */
    SyncCommand toCommand(YianlianSyncTask task);

    /**
     * 统计到期任务数
     */
    int countDue();

    /**
     * 管理端分页查询
     */
    List<YianlianSyncTask> selectSyncTaskList(YianlianSyncTask query);

    /**
     * 按主键查询（含 payload）
     */
    YianlianSyncTask selectSyncTaskById(Long taskId);
}
