package com.ruoyi.yianlian.client;

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

    @Autowired
    private IVpnLineAppService vpnLineAppService;

    public OpenApiClient(RestTemplateBuilder restTemplateBuilder, YiAnLianProperties yiAnLianProperties, RedisService redisService)
    {
        this.restTemplate = restTemplateBuilder.build();
        this.redisService = redisService;
    }

    public <T> T post(String appId, String path, Object body, Class<T> responseType)
        throws YiAnLianException
    {
        YiAnLianResponse<T> response = new YiAnLianResponse<T>();
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
                tokenVO = post(lineApp.getUrl() + YiAnLianConstants.tokenPath, request, null, TokenVO.class);
                if(tokenVO == null || StringUtils.isEmpty(tokenVO.getAccessToken())){
                    throw new YiAnLianException("获取易安联token失败");
                }
                redisService.setCacheObject(cacheKey, tokenVO);
            }
            String url = lineApp.getUrl() + path;
            response = post(url, body, tokenVO.getAccessToken(), YiAnLianResponse.class);
            if(response == null ){
                throw new YiAnLianException("易安联接口返回结果为空");
            }
            if(YiAnLianResultCode.SUCCESS.getCode() != response.getCode()) {
                throw new YiAnLianException(response.getMessages());
            }
            if(responseType == Boolean.class){
                return (T) Boolean.TRUE;
            }
        }
        catch (YiAnLianException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new YiAnLianException(e.getMessage());
        }
        return response.getData();
    }

    public <T> T post(String path, Object body, String accessToken, Class<T> responseType)
    {
        if (StringUtils.isEmpty(path))
        {
            throw new ServiceException("易安联接口路径未配置");
        }

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

        return response;
    }

    private String buildCacheKey(String appId)
    {
        return CacheConstants.YIANLIAN_TOKEN_KEY + appId;
    }
}
