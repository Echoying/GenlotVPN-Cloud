package com.ruoyi.yianlian.service.vpn;

import com.ruoyi.common.redis.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 线路密码轮换的互斥与幂等标记。
 *
 * 易安联改密链路可达十几秒，调用方超时重试时必须串行化：后到的请求等前一次结束，
 * 命中成功标记即复用结果，避免拿旧密码二次调用易安联导致「原密码不正确」。
 */
@Component
public class LinePasswordRotationGuard
{
    private static final Logger log = LoggerFactory.getLogger(LinePasswordRotationGuard.class);

    private static final String LOCK_KEY_PREFIX = "vpn:line_pwd_rotate:lock:";
    private static final String DONE_KEY_PREFIX = "vpn:line_pwd_rotate:done:";
    /** 锁自身存活时间：大于单次轮换耗时，进程异常退出后自动释放 */
    private static final long LOCK_TTL_SECONDS = 120L;
    /** 成功标记存活时间：覆盖客户端两次尝试的时间窗 */
    private static final long DONE_TTL_SECONDS = 120L;
    private static final long LOCK_RETRY_INTERVAL_MS = 200L;

    private final RedisService redisService;

    public LinePasswordRotationGuard(RedisService redisService)
    {
        this.redisService = redisService;
    }

    /** 最近是否已轮换成功（调用方据此直接返回成功，不再改密） */
    public boolean isRotatedRecently(Long localUserId, String appId)
    {
        return Boolean.TRUE.equals(redisService.hasKey(doneKey(localUserId, appId)));
    }

    public void markRotated(Long localUserId, String appId)
    {
        redisService.setCacheObject(doneKey(localUserId, appId), "1", DONE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 抢占轮换锁，最多等待 waitMs；超时返回 null 由调用方提示稍后重试
     */
    public String tryAcquire(Long localUserId, String appId, long waitMs)
    {
        String lockKey = lockKey(localUserId, appId);
        String token = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + waitMs;
        while (true)
        {
            Boolean ok = redisService.redisTemplate.opsForValue()
                .setIfAbsent(lockKey, token, LOCK_TTL_SECONDS, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(ok))
            {
                return token;
            }
            if (System.currentTimeMillis() >= deadline)
            {
                return null;
            }
            try
            {
                Thread.sleep(LOCK_RETRY_INTERVAL_MS);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return null;
            }
        }
    }

    public void release(Long localUserId, String appId, String token)
    {
        if (token == null)
        {
            return;
        }
        try
        {
            String lockKey = lockKey(localUserId, appId);
            Object current = redisService.redisTemplate.opsForValue().get(lockKey);
            if (token.equals(current))
            {
                redisService.deleteObject(lockKey);
            }
        }
        catch (Exception e)
        {
            log.warn("释放线路密码轮换锁异常（忽略）localUserId={}: {}", localUserId, e.getMessage());
        }
    }

    private String lockKey(Long localUserId, String appId)
    {
        return LOCK_KEY_PREFIX + localUserId + ":" + appId;
    }

    private String doneKey(Long localUserId, String appId)
    {
        return DONE_KEY_PREFIX + localUserId + ":" + appId;
    }
}
