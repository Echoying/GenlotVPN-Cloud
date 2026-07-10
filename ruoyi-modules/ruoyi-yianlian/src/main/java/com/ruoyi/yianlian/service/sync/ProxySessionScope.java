package com.ruoyi.yianlian.service.sync;

import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.yianlian.config.SyncProxyProperties;
import com.ruoyi.yianlian.constant.SyncProxyConstants;
import com.ruoyi.yianlian.domain.LineApp;
import com.ruoyi.yianlian.service.vpn.IVpnLineAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 代理会话作用域：统一管理「抢锁 → 登录 → 复用 → 退出 → 释放锁」生命周期。
 *
 * <p>同一线程内对同一代理（adminKey）的嵌套调用通过引用计数复用同一会话，
 * 仅在引用归零时退出登录并释放分布式锁，避免重复 login。直连线路直接执行业务。</p>
 */
@Component
public class ProxySessionScope
{
    private static final Logger log = LoggerFactory.getLogger(ProxySessionScope.class);

    private static final long LOCK_RETRY_INTERVAL_MS = 500L;

    private final ThreadLocal<Map<String, SessionHolder>> holders = ThreadLocal.withInitial(HashMap::new);

    @Autowired
    private IVpnLineAppService vpnLineAppService;

    @Autowired
    private SyncProxyProperties syncProxyProperties;

    @Autowired
    private SyncProxyEndpointResolver endpointResolver;

    @Autowired
    private ProxySessionService proxySessionService;

    @Autowired
    private RedisService redisService;

    /**
     * 以默认档位（实时 API）包裹执行业务
     */
    public <T> T run(String appId, SyncCallable<T> action) throws Exception
    {
        return run(appId, LoginRetryProfile.API, action);
    }

    /**
     * 包裹执行业务：代理线路先 login，业务完成后 logout（引用计数归零时）
     *
     * @param appId   线路ID
     * @param profile 登录重试档位
     * @param action  业务动作
     */
    public <T> T run(String appId, LoginRetryProfile profile, SyncCallable<T> action) throws Exception
    {
        LineApp lineApp = vpnLineAppService.getLineAppByAppId(appId);
        if (lineApp == null || !syncProxyProperties.isEnabled() || !SyncProxyConstants.isLineProxyEnabled(lineApp))
        {
            // 直连线路 / 全局未启用代理：无需会话，直接执行
            return action.call();
        }

        String adminKey = endpointResolver.resolveAdminKey(lineApp);
        Map<String, SessionHolder> map = holders.get();
        SessionHolder holder = map.get(adminKey);
        if (holder == null)
        {
            String lockToken = acquireLock(adminKey);
            try
            {
                markActive(adminKey);
                proxySessionService.loginWithRetry(lineApp, profile);
            }
            catch (RuntimeException e)
            {
                clearActive(adminKey);
                releaseLock(adminKey, lockToken);
                throw e;
            }
            holder = new SessionHolder(lockToken);
            map.put(adminKey, holder);
            waitAfterLogin(appId);
        }
        holder.refCount++;
        try
        {
            return action.call();
        }
        finally
        {
            holder.refCount--;
            if (holder.refCount <= 0)
            {
                map.remove(adminKey);
                try
                {
                    proxySessionService.logout(lineApp);
                }
                finally
                {
                    clearActive(adminKey);
                    releaseLock(adminKey, holder.lockToken);
                }
            }
        }
    }

    /**
     * 指定线路当前是否存在活跃同步会话（供线路探测跳过）
     */
    public boolean isActiveForLine(LineApp lineApp)
    {
        if (lineApp == null)
        {
            return false;
        }
        String activeKey = SyncProxyConstants.SESSION_ACTIVE_KEY_PREFIX + endpointResolver.resolveAdminKey(lineApp);
        return Boolean.TRUE.equals(redisService.hasKey(activeKey));
    }

    private String acquireLock(String adminKey)
    {
        String lockKey = SyncProxyConstants.SESSION_LOCK_KEY_PREFIX + adminKey;
        String token = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + syncProxyProperties.getSessionLockWaitMs();
        while (true)
        {
            Boolean ok = redisService.redisTemplate.opsForValue()
                .setIfAbsent(lockKey, token, syncProxyProperties.getSessionLockSeconds(), TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(ok))
            {
                return token;
            }
            if (System.currentTimeMillis() >= deadline)
            {
                throw new ProxyLoginException("获取代理会话锁超时（代理繁忙），adminKey=" + adminKey);
            }
            try
            {
                Thread.sleep(LOCK_RETRY_INTERVAL_MS);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new ProxyLoginException("获取代理会话锁被中断，adminKey=" + adminKey);
            }
        }
    }

    private void releaseLock(String adminKey, String token)
    {
        try
        {
            String lockKey = SyncProxyConstants.SESSION_LOCK_KEY_PREFIX + adminKey;
            Object current = redisService.redisTemplate.opsForValue().get(lockKey);
            if (token != null && token.equals(current))
            {
                redisService.deleteObject(lockKey);
            }
        }
        catch (Exception e)
        {
            log.warn("释放代理会话锁异常（忽略）adminKey={}: {}", adminKey, e.getMessage());
        }
    }

    private void markActive(String adminKey)
    {
        try
        {
            String activeKey = SyncProxyConstants.SESSION_ACTIVE_KEY_PREFIX + adminKey;
            redisService.setCacheObject(activeKey, "1", syncProxyProperties.getSessionLockSeconds(), TimeUnit.SECONDS);
        }
        catch (Exception e)
        {
            log.warn("设置代理会话活跃标记异常（忽略）adminKey={}: {}", adminKey, e.getMessage());
        }
    }

    private void clearActive(String adminKey)
    {
        try
        {
            redisService.deleteObject(SyncProxyConstants.SESSION_ACTIVE_KEY_PREFIX + adminKey);
        }
        catch (Exception e)
        {
            log.warn("清除代理会话活跃标记异常（忽略）adminKey={}: {}", adminKey, e.getMessage());
        }
    }

    /**
     * 代理 login 成功后等待隧道/上游路由就绪，再发起 OpenAPI 业务请求
     */
    private void waitAfterLogin(String appId)
    {
        long delayMs = syncProxyProperties.getPostLoginDelayMs();
        if (delayMs <= 0)
        {
            return;
        }
        log.info("代理线路[{}]登录成功，等待 {}ms 后发起业务请求", appId, delayMs);
        try
        {
            Thread.sleep(delayMs);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new ProxyLoginException("代理线路[" + appId + "]登录后等待被中断");
        }
    }

    /**
     * 线程内某个代理的会话持有者
     */
    private static final class SessionHolder
    {
        private final String lockToken;
        private int refCount;

        private SessionHolder(String lockToken)
        {
            this.lockToken = lockToken;
            this.refCount = 0;
        }
    }
}
