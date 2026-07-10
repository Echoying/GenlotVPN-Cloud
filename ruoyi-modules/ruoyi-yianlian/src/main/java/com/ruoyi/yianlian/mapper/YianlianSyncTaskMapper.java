package com.ruoyi.yianlian.mapper;

import com.ruoyi.yianlian.domain.YianlianSyncTask;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 易安联同步补偿任务 数据层
 */
public interface YianlianSyncTaskMapper
{
    /**
     * 新增任务
     */
    int insert(YianlianSyncTask task);

    /**
     * 更新任务
     */
    int update(YianlianSyncTask task);

    /**
     * 按主键查询
     */
    YianlianSyncTask selectById(@Param("taskId") Long taskId);

    /**
     * 查找同一 bizType+operation+appId+bizId 且待处理的任务（去重用）
     */
    YianlianSyncTask selectPendingDuplicate(@Param("bizType") String bizType,
                                            @Param("operation") String operation,
                                            @Param("appId") String appId,
                                            @Param("bizId") String bizId);

    /**
     * 查询到期可执行的 appId 列表（去重），按最小优先级排序
     */
    List<String> selectDueAppIds(@Param("limit") int limit);

    /**
     * 查询指定线路到期可执行的任务，按 priority、task_id 排序
     */
    List<YianlianSyncTask> selectDueTasksByAppId(@Param("appId") String appId, @Param("limit") int limit);

    /**
     * 锁定指定线路待抢占任务（事务内 FOR UPDATE）
     */
    List<YianlianSyncTask> selectDueTasksForClaim(@Param("appId") String appId, @Param("limit") int limit);

    /**
     * 将任务置为处理中（仅 status=0 可更新）
     */
    int claimTasks(@Param("taskIds") List<Long> taskIds);

    /**
     * 回收超时仍在处理中的任务，重新置为待处理
     */
    int resetStaleProcessing(@Param("staleMinutes") int staleMinutes);

    /**
     * 统计到期可执行任务数
     */
    int countDueTasks();

    /**
     * 管理端分页查询（列表不含 payload）
     */
    List<YianlianSyncTask> selectList(YianlianSyncTask query);
}
