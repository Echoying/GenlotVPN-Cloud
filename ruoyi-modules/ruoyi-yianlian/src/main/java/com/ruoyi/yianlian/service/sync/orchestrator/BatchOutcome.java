package com.ruoyi.yianlian.service.sync.orchestrator;

/**
 * 同线路批量执行结果类型
 */
public enum BatchOutcome
{
    /** 执行成功 */
    SUCCESS,
    /** 单条业务失败 */
    FAILED,
    /** 代理不可用，已入补偿队列 */
    DEFERRED
}
