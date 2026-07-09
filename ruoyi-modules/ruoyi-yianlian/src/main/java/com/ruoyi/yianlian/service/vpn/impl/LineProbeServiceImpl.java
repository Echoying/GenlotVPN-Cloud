package com.ruoyi.yianlian.service.vpn.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.yianlian.config.SyncProxyProperties;
import com.ruoyi.yianlian.domain.LineApp;
import com.ruoyi.yianlian.mapper.LineAppMapper;
import com.ruoyi.yianlian.service.vpn.ILineProbeService;
import com.ruoyi.yianlian.utils.AesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 线路探测：经 GenlotVPN-Proxy 管理 API 调用 30303 detect
 */
@Service
public class LineProbeServiceImpl implements ILineProbeService
{
    private static final Logger log = LoggerFactory.getLogger(LineProbeServiceImpl.class);

    private static final String PROBE_STATUS_SUCCESS = "1";
    private static final String PROBE_STATUS_FAIL = "2";
    private static final String MSG_PROXY_UNREACHABLE = "代理客户端无响应";

    private final LineAppMapper lineAppMapper;
    private final AesUtils aesUtils;
    private final SyncProxyProperties syncProxyProperties;
    private final RestTemplate restTemplate;

    public LineProbeServiceImpl(LineAppMapper lineAppMapper,
                                AesUtils aesUtils,
                                SyncProxyProperties syncProxyProperties,
                                RestTemplateBuilder restTemplateBuilder)
    {
        this.lineAppMapper = lineAppMapper;
        this.aesUtils = aesUtils;
        this.syncProxyProperties = syncProxyProperties;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofMillis(syncProxyProperties.getProbeReadTimeoutMs()))
                .build();
        // 探测接口：不因 HTTP 4xx/5xx 抛异常，统一解析 JSON 响应体
        this.restTemplate.setErrorHandler(new DefaultResponseErrorHandler()
        {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException
            {
                return false;
            }
        });
    }

    @Override
    public int runScheduledProbe(int maxCount)
    {
        if (maxCount <= 0)
        {
            return 0;
        }
        List<LineApp> lines = lineAppMapper.selectLinesForProbe(maxCount);
        if (lines == null || lines.isEmpty())
        {
            log.debug("线路探测：无待探测启用线路");
            return 0;
        }
        int probed = 0;
        for (LineApp line : lines)
        {
            probeOne(line);
            probed++;
        }
        log.info("线路探测完成，本次探测 {} 条", probed);
        return probed;
    }

    private void probeOne(LineApp line)
    {
        Date now = new Date();
        String probeUrl = buildProbeUrl();
        Map<String, Object> body = buildProbeBody(line);
        try
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(probeUrl, entity, String.class);
            saveProbeResult(line.getAppId(), parseProbeResponse(response.getBody()), now);
        }
        catch (RestClientException ex)
        {
            log.warn("线路 {} 探测失败，{}: {}", line.getAppId(), MSG_PROXY_UNREACHABLE, ex.getMessage());
            saveProbeFailure(line.getAppId(), MSG_PROXY_UNREACHABLE, now);
        }
        catch (Exception ex)
        {
            log.warn("线路 {} 探测异常: {}", line.getAppId(), ex.getMessage());
            saveProbeFailure(line.getAppId(), ex.getMessage(), now);
        }
    }

    private String buildProbeUrl()
    {
        String scheme = syncProxyProperties.getDefaultScheme();
        String host = syncProxyProperties.getDefaultHost();
        int port = syncProxyProperties.getProxyAdminPort();
        return String.format("%s://%s:%d/api/v1/probe", scheme, host, port);
    }

    private Map<String, Object> buildProbeBody(LineApp line)
    {
        Map<String, Object> lineMap = new LinkedHashMap<>();
        lineMap.put("host", line.getHost());
        lineMap.put("srvPort", line.getSrvPort() != null ? String.valueOf(line.getSrvPort()) : "");
        lineMap.put("spaPort", line.getSpaPort() != null ? String.valueOf(line.getSpaPort()) : "");
        String spaKeyMd5 = "";
        if (line.getSpaKey() != null && !line.getSpaKey().isEmpty())
        {
            spaKeyMd5 = AesUtils.md5(aesUtils.decrypt(line.getSpaKey()));
        }
        lineMap.put("spaKey", spaKeyMd5);
        Map<String, Object> body = new HashMap<>();
        body.put("line", lineMap);
        return body;
    }

    private ProbeParseResult parseProbeResponse(String responseBody)
    {
        if (responseBody == null || responseBody.isEmpty())
        {
            return ProbeParseResult.fail("代理返回空响应");
        }
        try
        {
            JSONObject json = JSON.parseObject(responseBody);
            int code = json.getIntValue("code");
            String msg = json.getString("msg");
            if (code != 200)
            {
                return ProbeParseResult.fail(msg != null && !msg.isEmpty() ? msg : "代理探测失败");
            }
            JSONObject data = json.getJSONObject("data");
            boolean available = data != null && data.getBooleanValue("available");
            if (available)
            {
                return ProbeParseResult.success();
            }
            return ProbeParseResult.fail(msg != null && !msg.isEmpty() ? msg : "线路不可用");
        }
        catch (Exception ex)
        {
            return ProbeParseResult.fail("代理返回异常响应");
        }
    }

    private void saveProbeResult(String appId, ProbeParseResult result, Date probeTime)
    {
        if (result.success)
        {
            LineApp update = new LineApp();
            update.setAppId(appId);
            update.setProbeStatus(PROBE_STATUS_SUCCESS);
            update.setProbeTime(probeTime);
            update.setProbeMsg(null);
            lineAppMapper.updateLineProbeResult(update);
            return;
        }
        saveProbeFailure(appId, result.message, probeTime);
    }

    private void saveProbeFailure(String appId, String message, Date probeTime)
    {
        LineApp update = new LineApp();
        update.setAppId(appId);
        update.setProbeStatus(PROBE_STATUS_FAIL);
        update.setProbeTime(probeTime);
        if (message != null && message.length() > 500)
        {
            message = message.substring(0, 500);
        }
        update.setProbeMsg(message);
        lineAppMapper.updateLineProbeResult(update);
    }

    private static final class ProbeParseResult
    {
        private final boolean success;
        private final String message;

        private ProbeParseResult(boolean success, String message)
        {
            this.success = success;
            this.message = message;
        }

        private static ProbeParseResult success()
        {
            return new ProbeParseResult(true, null);
        }

        private static ProbeParseResult fail(String message)
        {
            return new ProbeParseResult(false, message);
        }
    }
}
