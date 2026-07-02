package com.ruoyi.vpn.auth.service;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.constant.CacheConstants;
import com.ruoyi.common.core.utils.JwtUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.vpn.auth.context.ClientAuditContext;
import com.ruoyi.vpn.auth.tcp.TcpSessionContext;
import com.ruoyi.vpn.protocol.Envelope;
import com.ruoyi.vpn.protocol.ReportClientLoginRequest;
import com.ruoyi.yianlian.api.domain.VpnUserOnline;
import com.ruoyi.yianlian.api.domain.VpnUserInfo;
import com.ruoyi.yianlian.api.model.VpnLoginUser;

/**
 * VPN 在线会话注册表（Redis）
 */
@Component
public class VpnUserOnlineRegistryService
{
    @Autowired
    private RedisService redisService;

    /**
     * 客户端 connect 成功上报后注册在线会话
     */
    public void registerOnConnect(TcpSessionContext session, Envelope envelope, ReportClientLoginRequest req)
    {
        String accessToken = resolveAccessToken(envelope, session);
        if (StringUtils.isEmpty(accessToken))
        {
            return;
        }
        String tokenId = JwtUtils.getUserKey(accessToken);
        if (StringUtils.isEmpty(tokenId))
        {
            return;
        }

        String appId = StringUtils.isNotEmpty(req.getAppId()) ? req.getAppId() : session.getAppId();
        String appName = StringUtils.isNotEmpty(req.getAppName()) ? req.getAppName() : session.getAppName();

        VpnUserOnline online = new VpnUserOnline();
        online.setTokenId(tokenId);
        online.setUserId(session.getUserId());
        online.setUserName(session.getUsername());
        online.setAppId(appId);
        online.setAppName(appName);
        online.setLoginPurpose(session.getLoginPurpose());
        online.setLoginTime(System.currentTimeMillis());

        String ip = StringUtils.isNotEmpty(req.getClientIp()) ? req.getClientIp() : ClientAuditContext.resolveIpaddr();
        if (StringUtils.isEmpty(ip))
        {
            ip = session.getClientReportedIp();
        }
        if (StringUtils.isEmpty(ip))
        {
            ip = session.getClientIp();
        }
        online.setIpaddr(ip);
        online.setClientOs(StringUtils.isNotEmpty(req.getClientOs()) ? req.getClientOs() : session.getClientOs());
        online.setClientMac(StringUtils.isNotEmpty(req.getClientMac()) ? req.getClientMac() : session.getClientMac());

        VpnLoginUser loginUser = redisService.getCacheObject(CacheConstants.LOGIN_TOKEN_KEY + tokenId);
        if (loginUser != null && loginUser.getVpnUser() != null)
        {
            VpnUserInfo vpnUser = loginUser.getVpnUser();
            if (online.getUserId() == null)
            {
                online.setUserId(vpnUser.getUserId());
            }
            if (StringUtils.isEmpty(online.getUserName()))
            {
                online.setUserName(vpnUser.getUserName());
            }
            online.setNickName(vpnUser.getNickName());
        }

        redisService.setCacheObject(CacheConstants.VPN_ONLINE_KEY + tokenId, online,
                CacheConstants.EXPIRATION, TimeUnit.MINUTES);
    }

    /**
     * 按 access_token（JWT）注销在线会话
     */
    public void unregisterByAccessToken(String accessToken)
    {
        if (StringUtils.isEmpty(accessToken))
        {
            return;
        }
        String tokenId = JwtUtils.getUserKey(accessToken);
        if (StringUtils.isNotEmpty(tokenId))
        {
            redisService.deleteObject(CacheConstants.VPN_ONLINE_KEY + tokenId);
        }
    }

    /**
     * 按 tokenId 注销在线会话
     */
    public void unregisterByTokenId(String tokenId)
    {
        if (StringUtils.isNotEmpty(tokenId))
        {
            redisService.deleteObject(CacheConstants.VPN_ONLINE_KEY + tokenId);
        }
    }

    /**
     * 会话心跳时续期在线记录 TTL
     */
    public void touchOnlineSession(String accessToken)
    {
        if (StringUtils.isEmpty(accessToken))
        {
            return;
        }
        String tokenId = JwtUtils.getUserKey(accessToken);
        if (StringUtils.isEmpty(tokenId))
        {
            return;
        }
        String key = CacheConstants.VPN_ONLINE_KEY + tokenId;
        VpnUserOnline online = redisService.getCacheObject(key);
        if (online != null)
        {
            redisService.setCacheObject(key, online, CacheConstants.EXPIRATION, TimeUnit.MINUTES);
        }
    }

    private String resolveAccessToken(Envelope envelope, TcpSessionContext session)
    {
        if (envelope != null && StringUtils.isNotEmpty(envelope.getAccessToken()))
        {
            return envelope.getAccessToken();
        }
        return session != null ? session.getAccessToken() : null;
    }
}
