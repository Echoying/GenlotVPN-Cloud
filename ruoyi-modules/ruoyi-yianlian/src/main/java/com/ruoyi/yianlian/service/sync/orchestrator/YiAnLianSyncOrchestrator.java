package com.ruoyi.yianlian.service.sync.orchestrator;

import com.ruoyi.common.core.exception.yianlian.YiAnLianException;
import com.ruoyi.yianlian.domain.YianlianSyncTask;
import com.ruoyi.yianlian.service.sync.LoginRetryProfile;
import com.ruoyi.yianlian.service.sync.ProxyLoginException;
import com.ruoyi.yianlian.service.sync.ProxySessionScope;
import com.ruoyi.yianlian.service.sync.handler.SyncHandler;
import com.ruoyi.yianlian.service.sync.task.IYiAnLianSyncTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 易安联统一同步编排器：所有实体的同步入口。
 *
 * <p>核心原则：先在代理会话内完成远程 + 本地同步（远程失败本地不写/回滚）；
 * 实时 API 代理 login 失败则入队补偿并抛 {@link SyncDeferredException}；
 * 补偿 Job 按线路批处理，同线路仅 login 一次。</p>
 */
@Component
public class YiAnLianSyncOrchestrator
{
    private static final Logger log = LoggerFactory.getLogger(YiAnLianSyncOrchestrator.class);

    private final Map<String, SyncHandler> registry = new HashMap<>();
    private final ProxySessionScope proxySessionScope;
    private final IYiAnLianSyncTaskService taskService;

    /** 补偿任务最大重试次数（超过置为放弃） */
    @Value("${yianlian.sync.task-max-retries:48}")
    private int taskMaxRetries;

    public YiAnLianSyncOrchestrator(List<SyncHandler> handlers,
                                    ProxySessionScope proxySessionScope,
                                    IYiAnLianSyncTaskService taskService)
    {
        this.proxySessionScope = proxySessionScope;
        this.taskService = taskService;
        if (handlers != null)
        {
            for (SyncHandler handler : handlers)
            {
                registry.put(handler.bizType(), handler);
            }
        }
    }

    /**
     * 执行单条同步命令（实时 API 或 Job 单条重放）
     */
    public void execute(SyncCommand command)
    {
        SyncHandler handler = requireHandler(command.getBizType());
        boolean api = SyncConstants.SOURCE_API.equals(command.getSource());
        LoginRetryProfile profile = api ? LoginRetryProfile.API : LoginRetryProfile.RETRY_JOB;
        try
        {
            proxySessionScope.run(command.getAppId(), profile, () ->
            {
                handler.execute(command);
                return null;
            });
        }
        catch (ProxyLoginException e)
        {
            if (api && handler.deferrable(command))
            {
                Long taskId = taskService.enqueue(command, e.getMessage(), SyncConstants.priorityOf(command.getBizType()));
                throw new SyncDeferredException(taskId, "代理暂不可用，已加入补偿队列，约5分钟内自动重试");
            }
            throw e;
        }
        catch (YiAnLianException e)
        {
            throw e;
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new YiAnLianException(e.getMessage());
        }
    }

    /**
     * 实时 API 同线路批量执行：仅 login 一次，逐条处理，单条失败不中断其余任务。
     * 批次 login 失败时，deferrable 命令全部入补偿队列。
     *
     * @param appId    线路ID
     * @param commands 同线路命令列表
     * @return 与 commands 顺序一致的结果列表
     */
    public List<BatchItemResult> executeBatchApi(String appId, List<SyncCommand> commands)
    {
        if (commands == null || commands.isEmpty())
        {
            return Collections.emptyList();
        }
        List<BatchItemResult> results = new ArrayList<>(commands.size());
        try
        {
            proxySessionScope.run(appId, LoginRetryProfile.API, () ->
            {
                for (SyncCommand command : commands)
                {
                    results.add(runOneInSession(command));
                }
                return null;
            });
        }
        catch (ProxyLoginException e)
        {
            log.warn("同线路批量同步代理登录失败，deferrable 命令入队 appId={}: {}", appId, e.getMessage());
            for (SyncCommand command : commands)
            {
                results.add(deferOrFailOnLoginError(command, e));
            }
        }
        catch (YiAnLianException e)
        {
            throw e;
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new YiAnLianException(e.getMessage());
        }
        return results;
    }

    private BatchItemResult runOneInSession(SyncCommand command)
    {
        BatchItemResult result = new BatchItemResult(command);
        try
        {
            requireHandler(command.getBizType()).execute(command);
            result.setOutcome(BatchOutcome.SUCCESS);
        }
        catch (Exception e)
        {
            result.setOutcome(BatchOutcome.FAILED);
            result.setMessage(e.getMessage());
            result.setError(e);
            log.warn("同线路批量同步单条失败 appId={}, biz={}/{}: {}",
                command.getAppId(), command.getBizType(), command.getOperation(), e.getMessage());
        }
        return result;
    }

    private BatchItemResult deferOrFailOnLoginError(SyncCommand command, ProxyLoginException e)
    {
        BatchItemResult result = new BatchItemResult(command);
        SyncHandler handler = requireHandler(command.getBizType());
        if (handler.deferrable(command))
        {
            Long taskId = taskService.enqueue(command, e.getMessage(), SyncConstants.priorityOf(command.getBizType()));
            result.setOutcome(BatchOutcome.DEFERRED);
            result.setTaskId(taskId);
            result.setMessage("代理暂不可用，已加入补偿队列，约5分钟内自动重试");
        }
        else
        {
            result.setOutcome(BatchOutcome.FAILED);
            result.setMessage(e.getMessage());
            result.setError(e);
        }
        return result;
    }

    /**
     * 按线路批处理补偿任务：同线路仅 login 一次，逐条执行，最后 logout。
     * 批次 login 失败则整批延后重试。
     */
    public void executeBatch(String appId, List<YianlianSyncTask> tasks)
    {
        if (tasks == null || tasks.isEmpty())
        {
            return;
        }
        try
        {
            proxySessionScope.run(appId, LoginRetryProfile.RETRY_JOB, () ->
            {
                for (YianlianSyncTask task : tasks)
                {
                    processOneTaskInSession(task);
                }
                return null;
            });
        }
        catch (ProxyLoginException e)
        {
            log.warn("补偿批次代理登录失败，整批延后 appId={}: {}", appId, e.getMessage());
            taskService.deferBatch(tasks, e.getMessage(), taskMaxRetries);
        }
        catch (Exception e)
        {
            log.error("补偿批次执行异常，整批延后 appId={}: {}", appId, e.getMessage(), e);
            taskService.deferBatch(tasks, e.getMessage(), taskMaxRetries);
        }
    }

    private void processOneTaskInSession(YianlianSyncTask task)
    {
        try
        {
            SyncCommand command = taskService.toCommand(task);
            SyncHandler handler = requireHandler(command.getBizType());
            handler.execute(command);
            taskService.markSuccess(task);
        }
        catch (Exception e)
        {
            if (SyncRetryClassifier.isNonRetryable(e))
            {
                log.warn("补偿任务永久失败 taskId={}, biz={}/{}: {}",
                    task.getTaskId(), task.getBizType(), task.getOperation(), e.getMessage());
                taskService.markAbandoned(task, e.getMessage());
                return;
            }
            log.warn("补偿任务执行失败 taskId={}, biz={}/{}: {}",
                task.getTaskId(), task.getBizType(), task.getOperation(), e.getMessage());
            taskService.markFailure(task, e.getMessage(), taskMaxRetries);
        }
    }

    private SyncHandler requireHandler(String bizType)
    {
        SyncHandler handler = registry.get(bizType);
        if (handler == null)
        {
            throw new YiAnLianException("未注册的同步业务类型：" + bizType);
        }
        return handler;
    }
}
