package com.ruoyi.yianlian.service.sync;

import com.ruoyi.yianlian.domain.YianlianSyncTask;
import com.ruoyi.yianlian.service.sync.orchestrator.YiAnLianSyncOrchestrator;
import com.ruoyi.yianlian.service.sync.task.IYiAnLianSyncTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 易安联同步补偿执行器：按线路分批补偿到期任务（同线路仅 login 一次）。
 */
@Component
public class YiAnLianSyncRetryRunner
{
    private static final Logger log = LoggerFactory.getLogger(YiAnLianSyncRetryRunner.class);

    @Autowired
    private IYiAnLianSyncTaskService taskService;

    @Autowired
    private YiAnLianSyncOrchestrator syncOrchestrator;

    /** 单条线路单次批量处理的最大任务数 */
    @Value("${yianlian.sync.task-batch-max-per-run:200}")
    private int batchMaxPerRun;

    /** 处理中超时回收阈值（分钟），防止 Job 崩溃导致任务长期占用 */
    @Value("${yianlian.sync.task-stale-processing-minutes:30}")
    private int staleProcessingMinutes;

    /**
     * 执行一轮补偿
     *
     * @param maxAppIds 单次最多处理的线路数
     * @return 实际处理的任务条数
     */
    public int runPending(int maxAppIds)
    {
        taskService.resetStaleProcessing(staleProcessingMinutes);
        List<String> appIds = taskService.listDueAppIds(maxAppIds);
        if (appIds == null || appIds.isEmpty())
        {
            return 0;
        }
        int processed = 0;
        for (String appId : appIds)
        {
            List<YianlianSyncTask> tasks = taskService.claimDueTasksByAppId(appId, batchMaxPerRun);
            if (tasks == null || tasks.isEmpty())
            {
                continue;
            }
            log.info("补偿线路 appId={}, 本批任务 {} 条", appId, tasks.size());
            syncOrchestrator.executeBatch(appId, tasks);
            processed += tasks.size();
        }
        return processed;
    }
}
