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
 * 钉钉机器人通用发送（支持多套 Webhook 配置）
 */
@Component
public class DingTalkRobotSender
{
    private static final Logger log = LoggerFactory.getLogger(DingTalkRobotSender.class);

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DingTalkRobotResponse sendText(DingTalkWebhookConfig config, String content)
    {
        return sendText(config, content, null);
    }

    public DingTalkRobotResponse sendText(DingTalkWebhookConfig config, String content, DingTalkAt at)
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
        return send(config, body);
    }

    public DingTalkRobotResponse sendMarkdown(DingTalkWebhookConfig config, String title, String text)
    {
        return sendMarkdown(config, title, text, null);
    }

    public DingTalkRobotResponse sendMarkdown(DingTalkWebhookConfig config, String title, String text, DingTalkAt at)
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
        return send(config, body);
    }

    public DingTalkRobotResponse send(DingTalkWebhookConfig config, Map<String, Object> body)
    {
        checkEnabled(config);
        String url = buildRequestUrl(config);
        try
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String json = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            if (log.isDebugEnabled())
            {
                log.debug("钉钉机器人请求: url={}, body={}", maskUrlToken(url), json);
            }
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

    private void checkEnabled(DingTalkWebhookConfig config)
    {
        if (!config.isEnabled())
        {
            throw new ServiceException("钉钉机器人未启用");
        }
        if (StringUtils.isEmpty(config.getAccessToken()))
        {
            throw new ServiceException("钉钉机器人 accessToken 未配置");
        }
    }

    private String buildRequestUrl(DingTalkWebhookConfig config)
    {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(config.getWebhookUrl())
                .queryParam("access_token", config.getAccessToken());
        if (StringUtils.isNotEmpty(config.getSecret()))
        {
            long timestamp = System.currentTimeMillis();
            String sign = sign(timestamp, config.getSecret());
            builder.queryParam("timestamp", timestamp);
            builder.queryParam("sign", sign);
        }
        return builder.build(true).toUriString();
    }

    private String maskUrlToken(String url)
    {
        int idx = url.indexOf("access_token=");
        if (idx < 0)
        {
            return url;
        }
        return url.substring(0, idx + 13) + "***";
    }

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
}
