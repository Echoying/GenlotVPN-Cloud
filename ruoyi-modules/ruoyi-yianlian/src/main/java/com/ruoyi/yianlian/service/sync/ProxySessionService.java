package com.ruoyi.yianlian.service.sync;

import com.ruoyi.yianlian.domain.LineApp;

/**
 * 代理会话 HTTP 服务：登录 / 退出 GenlotVPN-Proxy 管理 API
 */
public interface ProxySessionService
{
    /**
     * 按重试档位登录代理（建立 30303 隧道）
     *
     * @param lineApp 线路
     * @param profile 重试档位（API 1 次 / RETRY_JOB 3 次）
     * @throws ProxyLoginException 重试耗尽仍失败
     */
    void loginWithRetry(LineApp lineApp, LoginRetryProfile profile);

    /**
     * 退出代理会话（best-effort，失败仅记日志）
     *
     * @param lineApp 线路
     */
    void logout(LineApp lineApp);
}
