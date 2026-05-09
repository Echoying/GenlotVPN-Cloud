package com.ruoyi.yianlian.client;

import com.fasterxml.jackson.core.type.TypeReference;
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

    private static final Logger log = LoggerFactory.getLogger(OpenApiClient.class);


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
            LineApp lineApp = vpnLineAppService.getLineAppByAppId(appId);
            if(lineApp == null){
                throw new YiAnLianException("未找到该线路");
            }
            // 先redis获取token
            /* 先缓存中获取 */
            String cacheKey = buildCacheKey(lineApp.getAppId());
            TokenVO tokenVO = redisService.getCacheObject(cacheKey);
            if(tokenVO == null || StringUtils.isEmpty(tokenVO.getAccessToken())){
                YiAnLianTokenRequest request = new YiAnLianTokenRequest();

                request.setAppId(lineApp.getAppId());
                request.setAppSecret(lineApp.getAppSecret());
                log.debug("获取易安联token, appId: {}", lineApp.getAppId());

                // 获取 token 也需要通过 YiAnLianResponse 包装
                String tokenResponseStr = post(lineApp.getUrl() + YiAnLianConstants.tokenPath, request, null, String.class);
                log.debug("易安联token接口原始响应: {}", tokenResponseStr);

                YiAnLianResponse<TokenVO> tokenResponse = objectMapper.readValue(
                    tokenResponseStr,
                    new TypeReference<YiAnLianResponse<TokenVO>>() {}
                );

                if(tokenResponse == null || tokenResponse.getData() == null){
                    throw new YiAnLianException("获取易安联token失败：响应为空");
                }
                if(YiAnLianResultCode.SUCCESS.getCode() != tokenResponse.getCode()) {
                    throw new YiAnLianException("获取易安联token失败：" + tokenResponse.getMessages());
                }

                tokenVO = tokenResponse.getData();
                if(tokenVO == null || StringUtils.isEmpty(tokenVO.getAccessToken())){
                    throw new YiAnLianException("获取易安联token失败：accessToken为空");
                }
                log.debug("易安联token获取成功, accessToken: {}, expireTime: {}秒", tokenVO.getAccessToken(), tokenVO.getExpireTime());
                // 使用 token 的过期时间设置缓存，提前 60 秒过期以避免边界问题
                Long expireTime = tokenVO.getExpireTime() != null && tokenVO.getExpireTime() > 60
                    ? Long.valueOf(tokenVO.getExpireTime() - 60)
                    : 7200L;
                redisService.setCacheObject(cacheKey, tokenVO, expireTime, java.util.concurrent.TimeUnit.SECONDS);
            }

            // 调用业务接口，同样需要手动反序列化泛型
            String url = lineApp.getUrl() + path;
            String responseStr = post(url, body, tokenVO.getAccessToken(), String.class);
            log.debug("易安联业务接口原始响应 - URL: {}, 响应: {}", url, responseStr);

            // 使用 ObjectMapper 手动反序列化泛型响应
            YiAnLianResponse<T> response = objectMapper.readValue(
                responseStr,
                objectMapper.getTypeFactory().constructParametricType(YiAnLianResponse.class, responseType)
            );

            if(response == null ){
                throw new YiAnLianException("易安联接口返回结果为空");
            }
            if(YiAnLianResultCode.SUCCESS.getCode() != response.getCode()) {
                throw new YiAnLianException(response.getMessages());
            }
            if(responseType == Boolean.class){
                return (T) Boolean.TRUE;
            }
            return response.getData();
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
        log.debug("调用易安联接口 - URL: {}, 请求参数: {}, accessToken: {}",  path, body, accessToken);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.isNotEmpty(accessToken))
        {
            headers.set("accessToken", accessToken);
        }

        T response = restTemplate.postForEntity(
                path,
                new HttpEntity<>(body, headers),
                responseType
        ).getBody();
        log.debug("易安联接口返回 - URL: {}, 响应数据: {}",  path, response);

        return response;
    }

    /**
     * GET 请求封装（带 token 认证）
     *
     * @param appId 应用ID
     * @param path 接口路径
     * @param responseType 响应类型
     * @param <T> 泛型类型
     * @return 响应数据
     * @throws YiAnLianException 易安联异常
     */
    public <T> T get(String appId, String path, Class<T> responseType)
        throws YiAnLianException
    {
        return get(appId, path, null, responseType);
    }

    /**
     * GET 请求封装（带查询参数和 token 认证）
     *
     * @param appId 应用ID
     * @param path 接口路径
     * @param queryParams 查询参数（可为null）
     * @param responseType 响应类型
     * @param <T> 泛型类型
     * @return 响应数据
     * @throws YiAnLianException 易安联异常
     */
    public <T> T get(String appId, String path, java.util.Map<String, Object> queryParams, Class<T> responseType)
        throws YiAnLianException
    {
        try
        {
            LineApp lineApp = vpnLineAppService.getLineAppByAppId(appId);
            if(lineApp == null){
                throw new YiAnLianException("未找到该线路");
            }

            // 获取 token
            String cacheKey = buildCacheKey(lineApp.getAppId());
            TokenVO tokenVO = redisService.getCacheObject(cacheKey);
            if(tokenVO == null || StringUtils.isEmpty(tokenVO.getAccessToken())){
                // 如果缓存中没有 token，先获取 token（复用 post 方法的逻辑）
                // 这里通过调用一个 POST 接口来触发 token 获取
                throw new YiAnLianException("Token未初始化，请先调用任意POST接口初始化token");
            }

            // 构建完整 URL（包含查询参数）
            String url = lineApp.getUrl() + path;
            if (queryParams != null && !queryParams.isEmpty()) {
                StringBuilder queryString = new StringBuilder("?");
                for (java.util.Map.Entry<String, Object> entry : queryParams.entrySet()) {
                    if (entry.getValue() != null) {
                        queryString.append(entry.getKey())
                                   .append("=")
                                   .append(java.net.URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"))
                                   .append("&");
                    }
                }
                // 移除最后一个 &
                if (queryString.length() > 1) {
                    queryString.setLength(queryString.length() - 1);
                    url += queryString.toString();
                }
            }

            // 调用 GET 接口
            String responseStr = doGet(url, tokenVO.getAccessToken(), String.class);
            log.debug("易安联GET接口原始响应 - URL: {}, 响应: {}", url, responseStr);

            // 使用 ObjectMapper 手动反序列化泛型响应
            YiAnLianResponse<T> response = objectMapper.readValue(
                responseStr,
                objectMapper.getTypeFactory().constructParametricType(YiAnLianResponse.class, responseType)
            );

            if(response == null ){
                throw new YiAnLianException("易安联接口返回结果为空");
            }
            if(YiAnLianResultCode.SUCCESS.getCode() != response.getCode()) {
                throw new YiAnLianException(response.getMessages());
            }
            if(responseType == Boolean.class){
                return (T) Boolean.TRUE;
            }
            return response.getData();
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

    /**
     * 底层 GET 请求方法
     *
     * @param url 完整URL
     * @param accessToken 访问令牌
     * @param responseType 响应类型
     * @param <T> 泛型类型
     * @return 响应数据
     */
    private <T> T doGet(String url, String accessToken, Class<T> responseType)
    {
        if (StringUtils.isEmpty(url))
        {
            throw new ServiceException("易安联接口路径未配置");
        }
        log.debug("调用易安联GET接口 - URL: {}, accessToken: {}",  url, accessToken);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.isNotEmpty(accessToken))
        {
            headers.set("accessToken", accessToken);
        }

        T response = restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                responseType
        ).getBody();
        log.debug("易安联GET接口返回 - URL: {}, 响应数据: {}",  url, response);

        return response;
    }

    private String buildCacheKey(String appId)
    {
        return CacheConstants.YIANLIAN_TOKEN_KEY + appId;
    }
}
