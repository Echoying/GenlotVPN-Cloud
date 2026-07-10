package com.ruoyi.yianlian.service.sync;

import com.ruoyi.common.core.exception.yianlian.YiAnLianException;

/**
 * 代理 login 重试耗尽异常：表示无法建立代理会话。
 * 实时 API 捕获后应将命令写入补偿队列；补偿 Job 捕获后应延后重试。
 */
public class ProxyLoginException extends YiAnLianException
{
    private static final long serialVersionUID = 1L;

    public ProxyLoginException(String errorMessage)
    {
        super(errorMessage);
    }
}
