package com.ruoyi.vpn.auth.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.vpn.auth.config.VpnLineVerifyProperties;
import com.ruoyi.vpn.auth.dingtalk.DingTalkRobotClient;
import com.ruoyi.vpn.auth.dingtalk.DingTalkRobotProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 选线钉钉验证码服务
 */
@Service
public class VpnLineVerifyService
{
    private static final Logger log = LoggerFactory.getLogger(VpnLineVerifyService.class);

    private static final String KEY_CODE = "vpn_line_verify:code:";
    private static final String KEY_COOLDOWN = "vpn_line_verify:cooldown:";
    private static final String KEY_ERR = "vpn_line_verify:err:";
    private static final String KEY_PASSED = "vpn_line_verify:passed:";

    @Autowired
    private RedisService redisService;

    @Autowired
    private DingTalkRobotClient dingTalkRobotClient;

    @Autowired
    private DingTalkRobotProperties dingTalkRobotProperties;

    @Autowired
    private VpnLineVerifyProperties verifyProperties;

    /**
     * 发送验证码到钉钉群
     */
    public Map<String, String> sendCode(Long userId, String username, String appId, String lineName)
    {
        validateAppId(appId);
        validateLineName(lineName);
        if (!dingTalkRobotProperties.isEnabled())
        {
            throw new ServiceException("钉钉机器人未启用，无法发送验证码");
        }

        String cooldownKey = keyCooldown(userId, appId);
        if (redisService.hasKey(cooldownKey))
        {
            throw new ServiceException("请" + verifyProperties.getSendCooldownSeconds() + "秒后再发送");
        }

        String code = generateCode();
        int ttlMinutes = verifyProperties.getCodeTtlMinutes();
        int validSeconds = ttlMinutes * 60;

        String codeKey = keyCode(userId, appId);
        redisService.setCacheObject(codeKey, code, (long) ttlMinutes, TimeUnit.MINUTES);
        redisService.setCacheObject(cooldownKey, "1", (long) verifyProperties.getSendCooldownSeconds(), TimeUnit.SECONDS);
        redisService.deleteObject(keyErr(userId, appId));

        Date expireAt = new Date(System.currentTimeMillis() + (long) validSeconds * 1000L);
        String expireAtStr = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, expireAt);

        String markdown = buildMarkdown(lineName, username, code, expireAtStr);
        dingTalkRobotClient.sendMarkdown("VPN线路验证码", markdown);
        log.info("选线验证码已发送 userId={} appId={} lineName={} expireAt={}", userId, appId, lineName, expireAtStr);

        Map<String, String> result = new HashMap<>();
        result.put("expireAt", expireAtStr);
        return result;
    }

    /**
     * 校验验证码
     */
    public void confirmCode(Long userId, String appId, String code)
    {
        validateAppId(appId);
        if (StringUtils.isEmpty(code))
        {
            throw new ServiceException("请输入验证码");
        }

        String codeKey = keyCode(userId, appId);
        String cachedCode = redisService.getCacheObject(codeKey);
        if (StringUtils.isEmpty(cachedCode))
        {
            throw new ServiceException("验证码错误或已过期");
        }

        if (!cachedCode.equals(code.trim()))
        {
            handleWrongCode(userId, appId, codeKey);
            throw new ServiceException("验证码错误或已过期");
        }

        redisService.deleteObject(codeKey);
        redisService.deleteObject(keyErr(userId, appId));
        int passedTtlMinutes = Math.max(verifyProperties.getCodeTtlMinutes(), 10);
        redisService.setCacheObject(keyPassed(userId, appId), "1", (long) passedTtlMinutes, TimeUnit.MINUTES);
        log.info("选线验证码校验通过 userId={} appId={}", userId, appId);
    }

    /**
     * 消费选线通过标记（获取控制器凭证前调用，一次性）
     */
    public void consumePassed(Long userId, String appId)
    {
        validateAppId(appId);
        String passedKey = keyPassed(userId, appId);
        if (!redisService.hasKey(passedKey))
        {
            throw new ServiceException("请先完成选线安全验证");
        }
        redisService.deleteObject(passedKey);
    }

    private void handleWrongCode(Long userId, String appId, String codeKey)
    {
        String errKey = keyErr(userId, appId);
        Integer errCount = redisService.getCacheObject(errKey);
        if (errCount == null)
        {
            errCount = 0;
        }
        errCount++;
        if (errCount >= verifyProperties.getMaxVerifyAttempts())
        {
            redisService.deleteObject(codeKey);
            redisService.deleteObject(errKey);
            log.warn("选线验证码错误次数超限 userId={} appId={}", userId, appId);
        }
        else
        {
            redisService.setCacheObject(errKey, errCount, (long) verifyProperties.getCodeTtlMinutes(), TimeUnit.MINUTES);
        }
    }

    private String buildMarkdown(String lineName, String username, String code, String expireAt)
    {
        return "### VPN 验证码\n\n"
            + "- **线路名称**：" + lineName + "\n\n"
            + "- **VPN 用户**：" + username + "\n\n"
            + "- **验证码**：" + code + "\n\n"
            + "- **有效时间截止**：" + expireAt;
    }

    private String generateCode()
    {
        int code = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(code);
    }

    private void validateAppId(String appId)
    {
        if (StringUtils.isEmpty(appId))
        {
            throw new ServiceException("线路标识不能为空");
        }
    }

    private void validateLineName(String lineName)
    {
        if (StringUtils.isEmpty(lineName))
        {
            throw new ServiceException("线路名称不能为空");
        }
        if (lineName.length() > verifyProperties.getLineNameMaxLength())
        {
            throw new ServiceException("线路名称过长");
        }
    }

    private String keyCode(Long userId, String appId)
    {
        return KEY_CODE + userId + ":" + appId;
    }

    private String keyCooldown(Long userId, String appId)
    {
        return KEY_COOLDOWN + userId + ":" + appId;
    }

    private String keyErr(Long userId, String appId)
    {
        return KEY_ERR + userId + ":" + appId;
    }

    private String keyPassed(Long userId, String appId)
    {
        return KEY_PASSED + userId + ":" + appId;
    }
}
