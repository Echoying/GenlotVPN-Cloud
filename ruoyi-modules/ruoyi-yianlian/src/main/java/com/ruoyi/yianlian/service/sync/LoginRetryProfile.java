package com.ruoyi.yianlian.service.sync;

/**
 * 代理 login 重试档位：实时 API 只尝试 1 次（失败入队补偿），补偿 Job 最多 3 次
 */
public enum LoginRetryProfile
{
    /** 实时 API 请求 */
    API,

    /** 定时补偿 Job */
    RETRY_JOB
}
