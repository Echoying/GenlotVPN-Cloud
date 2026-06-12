package com.ruoyi.vpn.auth.service;

import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.vpn.auth.dingtalk.DingTalkLoginNotifyProperties;
import com.ruoyi.vpn.auth.dingtalk.DingTalkRobotProperties;
import com.ruoyi.vpn.auth.dingtalk.DingTalkRobotSender;
import com.ruoyi.vpn.auth.dingtalk.DingTalkWebhookConfig;
import com.ruoyi.vpn.auth.dingtalk.dto.DingTalkRobotResponse;

/**
 * VPN 登录成功钉钉通知（异步，失败不影响登录）
 */
@Service
public class VpnLoginNotifyService
{
    private static final Logger log = LoggerFactory.getLogger(VpnLoginNotifyService.class);

    @Autowired
    private DingTalkLoginNotifyProperties notifyProperties;

    @Autowired
    private DingTalkRobotProperties robotProperties;

    @Autowired
    private DingTalkRobotSender dingTalkRobotSender;

    /**
     * 全程连接成功后异步推送钉钉通知
     */
    public void notifyConnectSuccess(LoginNotifyContext ctx)
    {
        DingTalkWebhookConfig config = resolveWebhookConfig();
        if (config == null)
        {
            return;
        }
        String robotSource = resolveRobotSource(config);
        CompletableFuture.runAsync(() -> sendNotify(ctx, config, robotSource));
    }

    /**
     * login-notify 已启用时仅走独立机器人，绝不回退 dingtalk.robot（验证码群）
     */
    private DingTalkWebhookConfig resolveWebhookConfig()
    {
        if (notifyProperties.isEnabled())
        {
            if (StringUtils.isEmpty(notifyProperties.getAccessToken()))
            {
                log.error("dingtalk.login-notify.enabled=true 但未配置 access-token，登录通知已跳过");
                return null;
            }
            if (looksLikeDingTalkSecret(notifyProperties.getAccessToken()))
            {
                log.error("dingtalk.login-notify.access-token 疑似填入了加签 secret（以 SEC 开头），"
                        + "请与 secret 字段对调后重试");
                return null;
            }
            return notifyProperties;
        }
        if (notifyProperties.isFallbackRobot() && robotProperties.isEnabled()
                && StringUtils.isNotEmpty(robotProperties.getAccessToken()))
        {
            log.info("login-notify 未启用，使用 dingtalk.robot 发送登录通知");
            return robotProperties;
        }
        log.info("登录成功钉钉通知跳过：dingtalk.login-notify 未启用");
        return null;
    }

    /** 钉钉加签 secret 以 SEC 开头，不应填入 access-token */
    private boolean looksLikeDingTalkSecret(String accessToken)
    {
        return StringUtils.isNotEmpty(accessToken) && accessToken.startsWith("SEC");
    }

    private String resolveRobotSource(DingTalkWebhookConfig config)
    {
        if (config instanceof DingTalkLoginNotifyProperties)
        {
            return "login-notify";
        }
        return "robot";
    }

    private void sendNotify(LoginNotifyContext ctx, DingTalkWebhookConfig config, String robotSource)
    {
        try
        {
            String markdown = buildMarkdown(ctx);
            DingTalkRobotResponse response = dingTalkRobotSender.sendMarkdown(config, "VPN 登录成功", markdown);
            log.info("登录成功钉钉通知已发送 user={} line={} robot={} tokenSuffix={} errcode={} errmsg={}",
                    ctx.getUsername(), ctx.getAppName(), robotSource, maskTokenSuffix(config.getAccessToken()),
                    response.getErrCode(), response.getErrMsg());
        }
        catch (Exception e)
        {
            log.warn("登录成功钉钉通知发送失败 user={} robot={} tokenSuffix={}",
                    ctx.getUsername(), robotSource, maskTokenSuffix(config.getAccessToken()), e);
        }
    }

    private String buildMarkdown(LoginNotifyContext ctx)
    {
        return "### VPN 登录成功\n\n"
                + "- **用户**：" + safe(ctx.getUsername()) + "\n\n"
                + "- **线路**：" + safe(ctx.getAppName()) + "\n\n"
                + "- **登录用途**：" + safe(ctx.getLoginPurpose()) + "\n\n"
                + "- **IP**：" + safe(ctx.getIpaddr()) + "\n\n"
                + "- **时间**：" + safe(ctx.getAccessTime());
    }

    private String maskTokenSuffix(String accessToken)
    {
        if (StringUtils.isEmpty(accessToken))
        {
            return "-";
        }
        return accessToken.length() <= 8 ? "***" : "..." + accessToken.substring(accessToken.length() - 8);
    }

    private String safe(String value)
    {
        return StringUtils.isEmpty(value) ? "-" : value;
    }
}
