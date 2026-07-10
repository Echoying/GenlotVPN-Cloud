package com.ruoyi.yianlian.service.sync.orchestrator;

/**
 * 同线路批量执行单条结果
 */
public class BatchItemResult
{
    private final SyncCommand command;
    private BatchOutcome outcome;
    private Long taskId;
    private String message;
    private Exception error;

    public BatchItemResult(SyncCommand command)
    {
        this.command = command;
    }

    public SyncCommand getCommand()
    {
        return command;
    }

    public BatchOutcome getOutcome()
    {
        return outcome;
    }

    public void setOutcome(BatchOutcome outcome)
    {
        this.outcome = outcome;
    }

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public Exception getError()
    {
        return error;
    }

    public void setError(Exception error)
    {
        this.error = error;
    }

    public boolean isSuccess()
    {
        return BatchOutcome.SUCCESS == outcome;
    }

    public boolean isDeferred()
    {
        return BatchOutcome.DEFERRED == outcome;
    }
}
