package com.ruoyi.yianlian.config;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncDeferredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 同步延迟异常处理：代理不可用时同步已入队补偿，返回业务码 202 + taskId，本地未变更。
 */
@RestControllerAdvice
public class YiAnLianSyncExceptionHandler
{
    private static final Logger log = LoggerFactory.getLogger(YiAnLianSyncExceptionHandler.class);

    /** 已加入补偿队列的业务码 */
    public static final int CODE_DEFERRED = 202;

    @ExceptionHandler(SyncDeferredException.class)
    public AjaxResult handleDeferred(SyncDeferredException e)
    {
        log.info("同步已延迟入队补偿 taskId={}, msg={}", e.getTaskId(), e.getMessage());
        AjaxResult result = new AjaxResult(CODE_DEFERRED, e.getMessage());
        result.put("taskId", e.getTaskId());
        return result;
    }
}
