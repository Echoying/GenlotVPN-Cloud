package com.ruoyi.yianlian.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.constant.CacheConstants;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.exception.yianlian.YiAnLianException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.yianlian.client.YiAnLianBase.YiAnLianResponse;
import com.ruoyi.yianlian.client.dto.YiAnLianTokenRequest;
import com.ruoyi.yianlian.client.dto.vo.TokenVO;
import com.ruoyi.yianlian.config.YiAnLianProperties;
import com.ruoyi.yianlian.constant.YiAnLianConstants;
import com.ruoyi.yianlian.constant.YiAnLianResultCode;
import com.ruoyi.yianlian.domain.LineApp;
import com.ruoyi.yianlian.service.vpn.IVpnLineAppService;
import com.ruoyi.yianlian.service.sync.SyncProxyEndpointResolver;
import com.ruoyi.yianlian.utils.AesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 易安联开放接口调用客户端
 */
@Component
public class OpenApiClient
{

    private final RestTemplate restTemplate;

    private final RedisService redisService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private IVpnLineAppService vpnLineAppService;

    @Autowired
    private AesUtils aesUtils;

    @Autowired
    private SyncProxyEndpointResolver syncProxyEndpointResolver;

    @Autowired
    private com.ruoyi.yianlian.service.sync.ProxySessionScope proxySessionScope;

    private static final Logger log = LoggerFactory.getLogger(OpenApiClient.class);

    private static final int TOKEN_RETRY_MAX = 2;

    public OpenApiClient(RestTemplateBuilder restTemplateBuilder, YiAnLianProperties yiAnLianProperties, RedisService redisService)
    {
        this.restTemplate = restTemplateBuilder.build();
        this.redisService = redisService;
    }

    public <T> T post(String appId, String path, Object body, Class<T> responseType)
        throws YiAnLianException
    {
        try
        {
            return proxySessionScope.run(appId, () ->
            {
                LineApp lineApp = requireLineApp(appId);
                String responseStr = invokePostWithTokenRetry(lineApp, path, body);

                if (responseType == Boolean.class)
                {
                    return parseBooleanResponse(responseStr);
                }

                YiAnLianResponse<T> response = objectMapper.readValue(
                    responseStr,
                    objectMapper.getTypeFactory().constructParametricType(YiAnLianResponse.class, responseType)
                );
                return requireSuccessData(response);
            });
        }
        catch (YiAnLianException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("调用易安联接口异常, appId: {}, path: {}, 错误: ", appId, path, e);
            throw new YiAnLianException(e.getMessage());
        }
    }

    public <T> java.util.List<T> postForList(String appId, String path, Object body, Class<T> elementType)
        throws YiAnLianException
    {
        try
        {
            return proxySessionScope.run(appId, () ->
            {
                LineApp lineApp = requireLineApp(appId);
                String responseStr = invokePostWithTokenRetry(lineApp, path, body);

                YiAnLianResponse<java.util.List<T>> response = objectMapper.readValue(
                    responseStr,
                    objectMapper.getTypeFactory().constructParametricType(
                        YiAnLianResponse.class,
                        objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, elementType)
                    )
                );
                return requireSuccessData(response);
            });
        }
        catch (YiAnLianException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("调用易安联接口异常, appId: {}, path: {}, 错误: ", appId, path, e);
            throw new YiAnLianException(e.getMessage());
        }
    }

    public <T> T post(String path, Object body, String accessToken, Class<T> responseType)
    {
        if (StringUtils.isEmpty(path))
        {
            throw new ServiceException("易安联接口路径未配置");
        }
        final boolean viaSyncProxy = isSyncProxyUrl(path);
        if (viaSyncProxy)
        {
            log.info("调用易安联接口(经同步代理) - URL: {}", path);
        }
        else
        {
            log.debug("调用易安联接口 - URL: {}, 请求参数: {}, accessToken: {}", path, body, accessToken);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.isNotEmpty(accessToken))
        {
            headers.set("accessToken", accessToken);
        }
        applySyncProxyHeaders(path, headers);

        T response = restTemplate.postForEntity(
            path,
            new HttpEntity<>(body, headers),
            responseType
        ).getBody();
        if (viaSyncProxy)
        {
            log.info("易安联接口返回(经同步代理) - URL: {}", path);
        }
        else
        {
            log.debug("易安联接口返回 - URL: {}, 响应数据: {}", path, response);
        }

        return response;
    }

    public <T> T get(String appId, String path, Class<T> responseType)
        throws YiAnLianException
    {
        return get(appId, path, null, responseType);
    }

    public <T> T get(String appId, String path, java.util.Map<String, Object> queryParams, Class<T> responseType)
        throws YiAnLianException
    {
        try
        {
            return proxySessionScope.run(appId, () ->
            {
                LineApp lineApp = requireLineApp(appId);
                String url = resolveApiBaseUrl(lineApp) + path;
                if (queryParams != null && !queryParams.isEmpty())
                {
                    StringBuilder queryString = new StringBuilder("?");
                    for (java.util.Map.Entry<String, Object> entry : queryParams.entrySet())
                    {
                        if (entry.getValue() != null)
                        {
                            queryString.append(entry.getKey())
                                .append("=")
                                .append(java.net.URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"))
                                .append("&");
                        }
                    }
                    if (queryString.length() > 1)
                    {
                        queryString.setLength(queryString.length() - 1);
                        url += queryString.toString();
                    }
                }

                String responseStr = invokeGetWithTokenRetry(lineApp, url);

                if (responseType == Boolean.class)
                {
                    return parseBooleanResponse(responseStr);
                }

                YiAnLianResponse<T> response = objectMapper.readValue(
                    responseStr,
                    objectMapper.getTypeFactory().constructParametricType(YiAnLianResponse.class, responseType)
                );
                return requireSuccessData(response);
            });
        }
        catch (YiAnLianException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("调用易安联GET接口异常, appId: {}, path: {}, 错误: ", appId, path, e);
            throw new YiAnLianException(e.getMessage());
        }
    }

    private LineApp requireLineApp(String appId) throws YiAnLianException
    {
        LineApp lineApp = vpnLineAppService.getLineAppByAppId(appId);
        if (lineApp == null)
        {
            throw new YiAnLianException("未找到该线路");
        }
        return lineApp;
    }

    private String invokePostWithTokenRetry(LineApp lineApp, String path, Object body) throws Exception
    {
        String url = resolveApiBaseUrl(lineApp) + path;
        for (int attempt = 0; attempt < TOKEN_RETRY_MAX; attempt++)
        {
            TokenVO tokenVO = obtainAccessToken(lineApp, attempt > 0);
            String responseStr = post(url, body, tokenVO.getAccessToken(), String.class);
            log.debug("易安联业务接口原始响应 - URL: {}, 响应: {}", url, responseStr);
            if (attempt == 0 && isAccessTokenUnavailable(responseStr))
            {
                log.warn("易安联 access_token 不可用，清除缓存并重新获取, appId: {}", lineApp.getAppId());
                redisService.deleteObject(buildCacheKey(lineApp.getAppId()));
                continue;
            }
            return responseStr;
        }
        throw new YiAnLianException("access_token不可用");
    }

    private String invokeGetWithTokenRetry(LineApp lineApp, String url) throws Exception
    {
        for (int attempt = 0; attempt < TOKEN_RETRY_MAX; attempt++)
        {
            TokenVO tokenVO = obtainAccessToken(lineApp, attempt > 0);
            String responseStr = doGet(url, tokenVO.getAccessToken(), String.class);
            log.debug("易安联GET接口原始响应 - URL: {}, 响应: {}", url, responseStr);
            if (attempt == 0 && isAccessTokenUnavailable(responseStr))
            {
                log.warn("易安联 access_token 不可用，清除缓存并重新获取, appId: {}", lineApp.getAppId());
                redisService.deleteObject(buildCacheKey(lineApp.getAppId()));
                continue;
            }
            return responseStr;
        }
        throw new YiAnLianException("access_token不可用");
    }

    private TokenVO obtainAccessToken(LineApp lineApp, boolean forceRefresh) throws Exception
    {
        String cacheKey = buildCacheKey(lineApp.getAppId());
        if (forceRefresh)
        {
            redisService.deleteObject(cacheKey);
        }
        else
        {
            TokenVO cached = redisService.getCacheObject(cacheKey);
            if (cached != null && StringUtils.isNotEmpty(cached.getAccessToken()))
            {
                return cached;
            }
        }

        YiAnLianTokenRequest request = new YiAnLianTokenRequest();
        request.setAppId(lineApp.getAppId());
        request.setAppSecret(aesUtils.decrypt(lineApp.getAppSecret()));
        log.debug("获取易安联token, appId: {}", lineApp.getAppId());

        String tokenResponseStr = post(resolveApiBaseUrl(lineApp) + YiAnLianConstants.tokenPath, request, null, String.class);
        log.debug("易安联token接口原始响应: {}", tokenResponseStr);

        int tokenCode = parseResponseCode(tokenResponseStr);
        if (tokenCode != YiAnLianResultCode.SUCCESS.getCode())
        {
            String msg = parseResponseMessage(tokenResponseStr);
            throw new YiAnLianException("获取易安联token失败：" + (StringUtils.isNotEmpty(msg) ? msg : "code=" + tokenCode));
        }

        YiAnLianResponse<TokenVO> tokenResponse = objectMapper.readValue(
            tokenResponseStr,
            new TypeReference<YiAnLianResponse<TokenVO>>() {}
        );
        TokenVO tokenVO = tokenResponse != null ? tokenResponse.getData() : null;
        if (tokenVO == null || StringUtils.isEmpty(tokenVO.getAccessToken()))
        {
            throw new YiAnLianException("获取易安联token失败：accessToken为空");
        }
        log.debug("易安联token获取成功, accessToken: {}, expireTime: {}秒", tokenVO.getAccessToken(), tokenVO.getExpireTime());

        long expireSeconds = tokenVO.getExpireTime() != null && tokenVO.getExpireTime() > 60
            ? tokenVO.getExpireTime() - 60L
            : 7140L;
        redisService.setCacheObject(cacheKey, tokenVO, expireSeconds, java.util.concurrent.TimeUnit.SECONDS);
        return tokenVO;
    }

    private boolean isAccessTokenUnavailable(String responseStr) throws Exception
    {
        return parseResponseCode(responseStr) == YiAnLianConstants.TOKEN_UNAVAILABLE_CODE;
    }

    private int parseResponseCode(String responseStr) throws Exception
    {
        if (StringUtils.isEmpty(responseStr))
        {
            return -1;
        }
        JsonNode root = objectMapper.readTree(responseStr);
        if (!root.has("code"))
        {
            return -1;
        }
        JsonNode codeNode = root.get("code");
        if (codeNode.isNumber())
        {
            return codeNode.asInt();
        }
        if (codeNode.isTextual())
        {
            try
            {
                return Integer.parseInt(codeNode.asText());
            }
            catch (NumberFormatException e)
            {
                return -1;
            }
        }
        return -1;
    }

    private String parseResponseMessage(String responseStr) throws Exception
    {
        if (StringUtils.isEmpty(responseStr))
        {
            return null;
        }
        JsonNode root = objectMapper.readTree(responseStr);
        if (root.has("messages") && !root.get("messages").isNull())
        {
            return root.get("messages").asText();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T parseBooleanResponse(String responseStr) throws Exception
    {
        YiAnLianResponse<Object> response = objectMapper.readValue(
            responseStr,
            objectMapper.getTypeFactory().constructParametricType(YiAnLianResponse.class, Object.class)
        );
        if (response == null)
        {
            throw new YiAnLianException("易安联接口返回结果为空");
        }
        if (YiAnLianResultCode.SUCCESS.getCode() != response.getCode())
        {
            throw new YiAnLianException(response.getMessages());
        }
        return (T) Boolean.TRUE;
    }

    private <T> T requireSuccessData(YiAnLianResponse<T> response) throws YiAnLianException
    {
        if (response == null)
        {
            throw new YiAnLianException("易安联接口返回结果为空");
        }
        if (YiAnLianResultCode.SUCCESS.getCode() != response.getCode())
        {
            throw new YiAnLianException(response.getMessages());
        }
        return response.getData();
    }

    private <T> T doGet(String url, String accessToken, Class<T> responseType)
    {
        if (StringUtils.isEmpty(url))
        {
            throw new ServiceException("易安联接口路径未配置");
        }
        final boolean viaSyncProxy = isSyncProxyUrl(url);
        if (viaSyncProxy)
        {
            log.info("调用易安联GET接口(经同步代理) - URL: {}", url);
        }
        else
        {
            log.debug("调用易安联GET接口 - URL: {}, accessToken: {}", url, accessToken);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.isNotEmpty(accessToken))
        {
            headers.set("accessToken", accessToken);
        }
        applySyncProxyHeaders(url, headers);

        T response = restTemplate.exchange(
            url,
            org.springframework.http.HttpMethod.GET,
            new HttpEntity<>(headers),
            responseType
        ).getBody();
        if (viaSyncProxy)
        {
            log.info("易安联GET接口返回(经同步代理) - URL: {}", url);
        }
        else
        {
            log.debug("易安联GET接口返回 - URL: {}, 响应数据: {}", url, response);
        }

        return response;
    }

    /**
     * 同步代理走 HTTP 短连接，禁止 HttpClient 复用已被代理关闭的连接
     */
    private boolean isSyncProxyUrl(String url)
    {
        return StringUtils.isNotEmpty(url)
            && url.regionMatches(true, 0, "http://", 0, 7);
    }

    private void applySyncProxyHeaders(String url, HttpHeaders headers)
    {
        if (isSyncProxyUrl(url))
        {
            headers.setConnection("close");
            headers.set("Accept-Encoding", "identity");
        }
    }

    private String buildCacheKey(String appId)
    {
        return CacheConstants.YIANLIAN_TOKEN_KEY + appId;
    }

    private String resolveApiBaseUrl(LineApp lineApp)
    {
        return syncProxyEndpointResolver.resolveApiBaseUrl(lineApp);
    }
}
