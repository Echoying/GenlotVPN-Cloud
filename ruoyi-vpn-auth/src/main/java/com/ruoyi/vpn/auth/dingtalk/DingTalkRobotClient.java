package com.ruoyi.vpn.auth.dingtalk;

import com.ruoyi.vpn.auth.dingtalk.dto.DingTalkAt;
import com.ruoyi.vpn.auth.dingtalk.dto.DingTalkRobotResponse;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 钉钉自定义机器人消息客户端（线路验证码等，使用 dingtalk.robot 配置）
 */
@Component
public class DingTalkRobotClient
{
    private final DingTalkRobotProperties properties;

    private final DingTalkRobotSender sender;

    public DingTalkRobotClient(DingTalkRobotProperties properties, DingTalkRobotSender sender)
    {
        this.properties = properties;
        this.sender = sender;
    }

    /**
     * 发送文本消息
     */
    public DingTalkRobotResponse sendText(String content)
    {
        return sendText(content, null);
    }

    /**
     * 发送文本消息（支持 @）
     */
    public DingTalkRobotResponse sendText(String content, DingTalkAt at)
    {
        return sender.sendText(properties, content, at);
    }

    /**
     * 发送 Markdown 消息
     */
    public DingTalkRobotResponse sendMarkdown(String title, String text)
    {
        return sendMarkdown(title, text, null);
    }

    /**
     * 发送 Markdown 消息（支持 @）
     */
    public DingTalkRobotResponse sendMarkdown(String title, String text, DingTalkAt at)
    {
        return sender.sendMarkdown(properties, title, text, at);
    }

    /**
     * 发送自定义消息体（msgtype 由调用方在 body 中指定）
     */
    public DingTalkRobotResponse send(Map<String, Object> body)
    {
        return sender.send(properties, body);
    }

    /**
     * 快捷构建 @ 指定手机号
     */
    public static DingTalkAt atMobiles(List<String> mobiles)
    {
        DingTalkAt at = new DingTalkAt();
        at.setAtMobiles(mobiles);
        return at;
    }

    /**
     * @ 所有人
     */
    public static DingTalkAt atAll()
    {
        DingTalkAt at = new DingTalkAt();
        at.setAtAll(true);
        return at;
    }
}
