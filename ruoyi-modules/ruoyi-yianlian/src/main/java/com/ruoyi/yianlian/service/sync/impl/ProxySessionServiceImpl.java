package com.ruoyi.yianlian.service.sync.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.yianlian.config.SyncProxyProperties;
import com.ruoyi.yianlian.domain.LineApp;
import com.ruoyi.yianlian.service.sync.LoginRetryProfile;
import com.ruoyi.yianlian.service.sync.ProxyLoginException;
import com.ruoyi.yianlian.service.sync.ProxySessionService;
import com.ruoyi.yianlian.service.sync.SyncProxyCredentialResolver;
import com.ruoyi.yianlian.service.sync.SyncProxyEndpointResolver;
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
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 代理会话 HTTP 服务实现：调用 GenlotVPN-Proxy 管理 API 完成 login / logout
 */
@Service
public class ProxySessionServiceImpl implements ProxySessionService
{
    private static final Logger log = LoggerFactory.getLogger(ProxySessionServiceImpl.class);

    private static final int PROXY_CODE_SUCCESS = 200;
    private static final int PROXY_CODE_BUSY = 409;

    private final SyncProxyProperties syncProxyProperties;
    private final SyncProxyEndpointResolver endpointResolver;
    private final SyncProxyCredentialResolver credentialResolver;
    private final AesUtils aesUtils;
    private final RestTemplate restTemplate;

    public ProxySessionServiceImpl(SyncProxyProperties syncProxyProperties,
                                   SyncProxyEndpointResolver endpointResolver,
                                   SyncProxyCredentialResolver credentialResolver,
                                   AesUtils aesUtils,
                                   RestTemplateBuilder restTemplateBuilder)
    {
        this.syncProxyProperties = syncProxyProperties;
        this.endpointResolver = endpointResolver;
        this.credentialResolver = credentialResolver;
        this.aesUtils = aesUtils;
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofMillis(syncProxyProperties.getLoginReadTimeoutMs()))
            .build();
        // 代理管理 API 返回 HTTP 200 + JSON code；关闭 4xx/5xx 抛异常，统一按 body.code 判定
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
    public void loginWithRetry(LineApp lineApp, LoginRetryProfile profile)
    {
        int maxAttempts = profile == LoginRetryProfile.API
            ? syncProxyProperties.getLoginMaxAttemptsApi()
            : syncProxyProperties.getLoginMaxAttemptsJob();
        if (maxAttempts < 1)
        {
            maxAttempts = 1;
        }
        long intervalMs = syncProxyProperties.getLoginRetryIntervalMs();
        String lastError = "未知错误";
        for (int attempt = 1; attempt <= maxAttempts; attempt++)
        {
            LoginResult result = loginOnce(lineApp);
            if (result.success)
            {
                log.info("代理线路[{}]登录成功（第{}/{}次）", lineApp.getAppId(), attempt, maxAttempts);
                return;
            }
            lastError = result.message;
            log.warn("代理线路[{}]登录失败（第{}/{}次）：{}", lineApp.getAppId(), attempt, maxAttempts, lastError);
            if (attempt < maxAttempts && intervalMs > 0)
            {
                try
                {
                    Thread.sleep(intervalMs);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    throw new ProxyLoginException("代理线路[" + lineApp.getAppId() + "]登录被中断");
                }
            }
        }
        throw new ProxyLoginException("代理线路[" + lineApp.getAppId() + "]登录失败：" + lastError);
    }

    @Override
    public void logout(LineApp lineApp)
    {
        String url = endpointResolver.resolveAdminBaseUrl(lineApp) + "/api/v1/logout";
        try
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(new LinkedHashMap<>(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            int code = parseCode(response.getBody());
            if (code == PROXY_CODE_SUCCESS)
            {
                log.info("代理线路[{}]已退出会话", lineApp.getAppId());
            }
            else
            {
                log.warn("代理线路[{}]退出会话返回非成功码：{}", lineApp.getAppId(), response.getBody());
            }
        }
        catch (Exception e)
        {
            log.warn("代理线路[{}]退出会话异常（忽略）：{}", lineApp.getAppId(), e.getMessage());
        }
    }

    private LoginResult loginOnce(LineApp lineApp)
    {
        String url = endpointResolver.resolveAdminBaseUrl(lineApp) + "/api/v1/login";
        try
        {
            Map<String, Object> body = buildLoginBody(lineApp);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            String responseBody = response.getBody();
            int code = parseCode(responseBody);
            if (code == PROXY_CODE_SUCCESS)
            {
                return LoginResult.ok();
            }
            String msg = parseMsg(responseBody);
            if (code == PROXY_CODE_BUSY)
            {
                // 会话被占用：主动 logout 清理，便于后续重试
                log.warn("代理线路[{}]会话忙，尝试清理后重试", lineApp.getAppId());
                logout(lineApp);
            }
            return LoginResult.fail(msg != null ? msg : ("code=" + code));
        }
        catch (Exception e)
        {
            return LoginResult.fail(e.getMessage());
        }
    }

    private Map<String, Object> buildLoginBody(LineApp lineApp)
    {
        SyncProxyCredentialResolver.ProxyCredential credential = credentialResolver.resolve(lineApp.getAppId());

        Map<String, Object> lineMap = new LinkedHashMap<>();
        lineMap.put("host", lineApp.getHost());
        lineMap.put("srvPort", lineApp.getSrvPort() != null ? String.valueOf(lineApp.getSrvPort()) : "");
        lineMap.put("spaPort", lineApp.getSpaPort() != null ? String.valueOf(lineApp.getSpaPort()) : "");
        String spaKeyMd5 = "";
        if (lineApp.getSpaKey() != null && !lineApp.getSpaKey().isEmpty())
        {
            spaKeyMd5 = AesUtils.md5(aesUtils.decrypt(lineApp.getSpaKey()));
        }
        lineMap.put("spaKey", spaKeyMd5);

        Map<String, Object> proxyMap = new LinkedHashMap<>();
        proxyMap.put("upstreamUrl", lineApp.getUrl() != null ? lineApp.getUrl().trim() : "");
        List<String> allowedIps = new ArrayList<>();
        if (syncProxyProperties.getAllowedSourceIps() != null)
        {
            for (String ip : syncProxyProperties.getAllowedSourceIps())
            {
                if (ip != null && !ip.trim().isEmpty())
                {
                    allowedIps.add(ip.trim());
                }
            }
        }
        proxyMap.put("allowedSourceIps", allowedIps);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", credential.getUsername());
        body.put("password", credential.getPassword());
        body.put("line", lineMap);
        body.put("proxy", proxyMap);
        return body;
    }

    private int parseCode(String responseBody)
    {
        if (responseBody == null || responseBody.isEmpty())
        {
            return -1;
        }
        try
        {
            JSONObject json = JSON.parseObject(responseBody);
            return json.getIntValue("code");
        }
        catch (Exception e)
        {
            return -1;
        }
    }

    private String parseMsg(String responseBody)
    {
        if (responseBody == null || responseBody.isEmpty())
        {
            return null;
        }
        try
        {
            JSONObject json = JSON.parseObject(responseBody);
            return json.getString("msg");
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private static final class LoginResult
    {
        private final boolean success;
        private final String message;

        private LoginResult(boolean success, String message)
        {
            this.success = success;
            this.message = message;
        }

        private static LoginResult ok()
        {
            return new LoginResult(true, null);
        }

        private static LoginResult fail(String message)
        {
            return new LoginResult(false, message == null ? "代理无响应" : message);
        }
    }
}
