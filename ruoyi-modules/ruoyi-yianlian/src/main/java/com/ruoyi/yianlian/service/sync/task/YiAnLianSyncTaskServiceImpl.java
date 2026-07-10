package com.ruoyi.yianlian.service.sync.task;

import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.domain.YianlianSyncTask;
import com.ruoyi.yianlian.mapper.YianlianSyncTaskMapper;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 易安联同步补偿任务服务实现
 */
@Service
public class YiAnLianSyncTaskServiceImpl implements IYiAnLianSyncTaskService
{
    private static final Logger log = LoggerFactory.getLogger(YiAnLianSyncTaskServiceImpl.class);

    /** 下次重试延迟（毫秒），与 Job 周期一致（5 分钟） */
    private static final long RETRY_DELAY_MS = 5 * 60 * 1000L;

    @Autowired
    private YianlianSyncTaskMapper taskMapper;

    @Autowired
    private SyncTaskBizNameResolver bizNameResolver;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long enqueue(SyncCommand command, String error, int priority)
    {
        // 仅在有业务主键时去重（UPDATE/DELETE 等）；CREATE 无 bizId，不能去重，否则同线路多次新建会被误合并
        boolean canDedup = command.getBizId() != null && !command.getBizId().isEmpty();
        if (canDedup)
        {
            YianlianSyncTask existing = taskMapper.selectPendingDuplicate(
                command.getBizType(), command.getOperation(), command.getAppId(), command.getBizId());
            if (existing != null)
            {
                existing.setPayload(command.getPayload());
                existing.setLastError(truncate(error));
                existing.setNextRetryTime(new Date());
                taskMapper.update(existing);
                log.info("补偿任务已存在，更新 payload taskId={}, biz={}/{}, appId={}",
                    existing.getTaskId(), command.getBizType(), command.getOperation(), command.getAppId());
                return existing.getTaskId();
            }
        }
        YianlianSyncTask task = new YianlianSyncTask();
        task.setBizType(command.getBizType());
        task.setOperation(command.getOperation());
        task.setAppId(command.getAppId());
        task.setBizId(command.getBizId());
        task.setPayload(command.getPayload());
        task.setStatus(YianlianSyncTask.STATUS_PENDING);
        task.setRetryCount(0);
        task.setPriority(priority);
        task.setLastError(truncate(error));
        task.setNextRetryTime(new Date());
        task.setCreateBy("system");
        taskMapper.insert(task);
        log.info("新建补偿任务 taskId={}, biz={}/{}, appId={}",
            task.getTaskId(), command.getBizType(), command.getOperation(), command.getAppId());
        return task.getTaskId();
    }

    @Override
    public List<String> listDueAppIds(int limit)
    {
        return taskMapper.selectDueAppIds(limit);
    }

    @Override
    public List<YianlianSyncTask> listDueTasksByAppId(String appId, int limit)
    {
        return taskMapper.selectDueTasksByAppId(appId, limit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<YianlianSyncTask> claimDueTasksByAppId(String appId, int limit)
    {
        List<YianlianSyncTask> candidates = taskMapper.selectDueTasksForClaim(appId, limit);
        if (candidates == null || candidates.isEmpty())
        {
            return new ArrayList<>();
        }
        List<Long> taskIds = candidates.stream()
            .map(YianlianSyncTask::getTaskId)
            .collect(Collectors.toList());
        int claimed = taskMapper.claimTasks(taskIds);
        if (claimed <= 0)
        {
            return new ArrayList<>();
        }
        for (YianlianSyncTask task : candidates)
        {
            task.setStatus(YianlianSyncTask.STATUS_PROCESSING);
        }
        log.info("抢占补偿任务 appId={}, 本批 {} 条", appId, claimed);
        return candidates;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int resetStaleProcessing(int staleMinutes)
    {
        if (staleMinutes <= 0)
        {
            return 0;
        }
        int rows = taskMapper.resetStaleProcessing(staleMinutes);
        if (rows > 0)
        {
            log.warn("回收超时处理中补偿任务 {} 条（超过 {} 分钟）", rows, staleMinutes);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markSuccess(YianlianSyncTask task)
    {
        YianlianSyncTask update = new YianlianSyncTask();
        update.setTaskId(task.getTaskId());
        update.setStatus(YianlianSyncTask.STATUS_SUCCESS);
        update.setLastError("");
        taskMapper.update(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markFailure(YianlianSyncTask task, String error, int maxRetries)
    {
        int retry = (task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1;
        YianlianSyncTask update = new YianlianSyncTask();
        update.setTaskId(task.getTaskId());
        update.setRetryCount(retry);
        update.setLastError(truncate(error));
        if (maxRetries > 0 && retry >= maxRetries)
        {
            update.setStatus(YianlianSyncTask.STATUS_ABANDONED);
            log.warn("补偿任务达最大重试次数，置为放弃 taskId={}, retry={}", task.getTaskId(), retry);
        }
        else
        {
            update.setStatus(YianlianSyncTask.STATUS_PENDING);
            update.setNextRetryTime(new Date(System.currentTimeMillis() + RETRY_DELAY_MS));
        }
        taskMapper.update(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAbandoned(YianlianSyncTask task, String error)
    {
        YianlianSyncTask update = new YianlianSyncTask();
        update.setTaskId(task.getTaskId());
        update.setStatus(YianlianSyncTask.STATUS_ABANDONED);
        update.setLastError(truncate(error));
        taskMapper.update(update);
        log.warn("补偿任务永久失败，已放弃 taskId={}, biz={}/{}: {}",
            task.getTaskId(), task.getBizType(), task.getOperation(), truncate(error));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deferBatch(List<YianlianSyncTask> tasks, String error, int maxRetries)
    {
        if (tasks == null || tasks.isEmpty())
        {
            return;
        }
        for (YianlianSyncTask task : tasks)
        {
            markFailure(task, error, maxRetries);
        }
    }

    @Override
    public SyncCommand toCommand(YianlianSyncTask task)
    {
        return new SyncCommand(task.getBizType(), task.getOperation(), task.getAppId(),
            task.getBizId(), task.getPayload(), com.ruoyi.yianlian.service.sync.orchestrator.SyncConstants.SOURCE_RETRY_JOB);
    }

    @Override
    public int countDue()
    {
        return taskMapper.countDueTasks();
    }

    @Override
    public List<YianlianSyncTask> selectSyncTaskList(YianlianSyncTask query)
    {
        if (StringUtils.isNotEmpty(query.getAppIds()))
        {
            query.getParams().put("appIdList", Convert.toStrArray(query.getAppIds()));
        }
        List<YianlianSyncTask> list = taskMapper.selectList(query);
        if (list != null)
        {
            for (YianlianSyncTask task : list)
            {
                bizNameResolver.enrich(task);
            }
        }
        return list;
    }

    @Override
    public YianlianSyncTask selectSyncTaskById(Long taskId)
    {
        YianlianSyncTask task = taskMapper.selectById(taskId);
        bizNameResolver.enrich(task);
        return task;
    }

    private String truncate(String s)
    {
        if (s == null)
        {
            return null;
        }
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
