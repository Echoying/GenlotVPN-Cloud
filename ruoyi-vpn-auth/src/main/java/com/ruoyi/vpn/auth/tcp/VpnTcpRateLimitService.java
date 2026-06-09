package com.ruoyi.vpn.auth.tcp;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.vpn.auth.config.VpnTcpProperties;

/**
 * VPN TCP 登录频率限制（Redis，按 IP）
 */
@Service
public class VpnTcpRateLimitService
{
    private static final Logger log = LoggerFactory.getLogger(VpnTcpRateLimitService.class);

    private static final String KEY_LOGIN_RATE = "vpn_tcp:login_rate:";

    @Autowired
    private RedisService redisService;

    @Autowired
    private VpnTcpProperties properties;

    /**
     * 记录一次 Login 尝试；超过窗口内上限返回 false
     */
    public boolean tryAcquireLogin(String clientIp)
    {
        VpnTcpProperties.Limits limits = properties.getLimits();
        int maxAttempts = limits.getLoginMaxAttemptsPerIp();
        int windowSeconds = limits.getLoginRateWindowSeconds();
        if (maxAttempts <= 0 || windowSeconds <= 0 || StringUtils.isEmpty(clientIp))
        {
            return true;
        }

        String key = KEY_LOGIN_RATE + clientIp;
        Integer count = redisService.getCacheObject(key);
        if (count == null)
        {
            redisService.setCacheObject(key, 1, (long) windowSeconds, TimeUnit.SECONDS);
            return true;
        }
        if (count >= maxAttempts)
        {
            log.warn("TCP Login 频率超限 ip={} attempts={} windowSec={}", clientIp, count, windowSeconds);
            return false;
        }
        long ttl = redisService.getExpire(key);
        if (ttl <= 0)
        {
            ttl = windowSeconds;
        }
        redisService.setCacheObject(key, count + 1, ttl, TimeUnit.SECONDS);
        return true;
    }
}
