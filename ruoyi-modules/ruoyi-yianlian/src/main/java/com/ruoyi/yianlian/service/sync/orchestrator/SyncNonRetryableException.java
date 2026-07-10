package com.ruoyi.yianlian.service.sync.orchestrator;

import com.ruoyi.common.core.exception.yianlian.YiAnLianException;

/**
 * 补偿重放时不可重试的业务错误（配置缺失、前置依赖未满足、数据冲突等）。
 * 捕获后应立即放弃任务，避免无意义轮询。
 */
public class SyncNonRetryableException extends YiAnLianException
{
    private static final long serialVersionUID = 1L;

    public SyncNonRetryableException(String message)
    {
        super(message);
    }
}
