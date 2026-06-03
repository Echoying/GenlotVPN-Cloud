package com.ruoyi.vpn.auth.dingtalk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.vpn.auth.dingtalk.dto.DingTalkAt;
import com.ruoyi.vpn.auth.dingtalk.dto.DingTalkRobotResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 钉钉自定义机器人消息客户端
 * <p>
 * 对应接口：POST https://oapi.dingtalk.com/robot/send?access_token=xxx
 * 加签：附加 timestamp、sign 参数
 */
@Component
public class DingTalkRobotClient
{
    private static final Logger log = LoggerFactory.getLogger(DingTalkRobotClient.class);

    private final DingTalkRobotProperties properties;

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DingTalkRobotClient(DingTalkRobotProperties properties)
    {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
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
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msgtype", "text");
        Map<String, Object> text = new HashMap<>();
        text.put("content", content);
        body.put("text", text);
        if (at != null)
        {
            body.put("at", at);
        }
        return send(body);
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
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msgtype", "markdown");
        Map<String, Object> markdown = new HashMap<>();
        markdown.put("title", title);
        markdown.put("text", text);
        body.put("markdown", markdown);
        if (at != null)
        {
            body.put("at", at);
        }
        return send(body);
    }

    /**
     * 发送自定义消息体（msgtype 由调用方在 body 中指定）
     */
    public DingTalkRobotResponse send(Map<String, Object> body)
    {
        checkEnabled();
        String url = buildRequestUrl();
        try
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String json = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            log.debug("钉钉机器人请求: url={}, body={}", url, json);
            ResponseEntity<DingTalkRobotResponse> response = restTemplate.postForEntity(
                url, entity, DingTalkRobotResponse.class);
            DingTalkRobotResponse result = response.getBody();
            if (result == null)
            {
                throw new ServiceException("钉钉机器人返回为空");
            }
            if (!result.isSuccess())
            {
                throw new ServiceException("钉钉机器人发送失败: " + result.getErrMsg());
            }
            return result;
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("钉钉机器人发送异常", e);
            throw new ServiceException("钉钉机器人发送异常: " + e.getMessage());
        }
    }

    private void checkEnabled()
    {
        if (!properties.isEnabled())
        {
            throw new ServiceException("钉钉机器人未启用");
        }
        if (StringUtils.isEmpty(properties.getAccessToken()))
        {
            throw new ServiceException("钉钉机器人 accessToken 未配置");
        }
    }

    private String buildRequestUrl()
    {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.getWebhookUrl())
            .queryParam("access_token", properties.getAccessToken());
        if (StringUtils.isNotEmpty(properties.getSecret()))
        {
            long timestamp = System.currentTimeMillis();
            String sign = sign(timestamp, properties.getSecret());
            builder.queryParam("timestamp", timestamp);
            builder.queryParam("sign", sign);
        }
        return builder.build(true).toUriString();
    }

    /**
     * 加签：timestamp + "\n" + secret，HmacSHA256 后 Base64 再 URL 编码
     */
    private String sign(long timestamp, String secret)
    {
        try
        {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return URLEncoder.encode(Base64.getEncoder().encodeToString(signData), StandardCharsets.UTF_8.name());
        }
        catch (Exception e)
        {
            throw new ServiceException("钉钉机器人加签失败: " + e.getMessage());
        }
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
