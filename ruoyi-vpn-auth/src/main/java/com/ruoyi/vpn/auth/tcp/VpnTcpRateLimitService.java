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

    private static final String KEY_FEEDBACK_UPLOAD = "vpn_tcp:feedback_upload:";

    private static final String KEY_FEEDBACK_SUBMIT = "vpn_tcp:feedback_submit:";

    private static final int FEEDBACK_WINDOW_SECONDS = 3600;

    private static final int FEEDBACK_UPLOAD_MAX = 20;

    private static final int FEEDBACK_SUBMIT_MAX = 5;

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
        return tryAcquire(KEY_LOGIN_RATE, clientIp, limits.getLoginMaxAttemptsPerIp(),
                limits.getLoginRateWindowSeconds(), "TCP Login");
    }

    /**
     * 反馈截图上传：窗口 3600s、上限 20；空 IP 放行。
     */
    public boolean tryAcquireFeedbackUpload(String ip)
    {
        return tryAcquire(KEY_FEEDBACK_UPLOAD, ip, FEEDBACK_UPLOAD_MAX, FEEDBACK_WINDOW_SECONDS,
                "TCP FeedbackUpload");
    }

    /**
     * 反馈提交：窗口 3600s、上限 5；空 IP 放行。
     */
    public boolean tryAcquireFeedbackSubmit(String ip)
    {
        return tryAcquire(KEY_FEEDBACK_SUBMIT, ip, FEEDBACK_SUBMIT_MAX, FEEDBACK_WINDOW_SECONDS,
                "TCP FeedbackSubmit");
    }

    private boolean tryAcquire(String keyPrefix, String clientIp, int maxAttempts, int windowSeconds,
            String logLabel)
    {
        if (maxAttempts <= 0 || windowSeconds <= 0 || StringUtils.isEmpty(clientIp))
        {
            return true;
        }

        String key = keyPrefix + clientIp;
        Integer count = redisService.getCacheObject(key);
        if (count == null)
        {
            redisService.setCacheObject(key, 1, (long) windowSeconds, TimeUnit.SECONDS);
            return true;
        }
        if (count >= maxAttempts)
        {
            log.warn("{} 频率超限 ip={} attempts={} windowSec={}", logLabel, clientIp, count, windowSeconds);
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
