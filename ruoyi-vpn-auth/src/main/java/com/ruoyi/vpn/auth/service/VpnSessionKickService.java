package com.ruoyi.vpn.auth.service;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.constant.CacheConstants;
import com.ruoyi.common.core.utils.JwtUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.vpn.auth.domain.VpnSessionIndex;
import com.ruoyi.yianlian.api.model.VpnLoginUser;

/**
 * VPN 用户全局单会话：新登录挤掉旧 token
 */
@Component
public class VpnSessionKickService
{
    private static final Logger log = LoggerFactory.getLogger(VpnSessionKickService.class);

    public static final String KICKED_MSG = "账号已在其他设备登录，请重新登录";

    @Autowired
    private RedisService redisService;

    /**
     * 新登录前踢掉该用户全部 VPN 云端会话（须在 createToken 之前调用）
     */
    public void kickAllSessionsForUser(Long localUserId)
    {
        if (localUserId == null)
        {
            return;
        }
        Collection<String> keys = redisService.keys(CacheConstants.LOGIN_TOKEN_KEY + "*");
        if (keys == null || keys.isEmpty())
        {
            keys = redisService.scanKeys(CacheConstants.LOGIN_TOKEN_KEY + "*");
        }
        if (keys == null || keys.isEmpty())
        {
            return;
        }
        String prefix = CacheConstants.LOGIN_TOKEN_KEY;
        for (String key : keys)
        {
            if (StringUtils.isEmpty(key) || !key.startsWith(prefix))
            {
                continue;
            }
            String tokenId = key.substring(prefix.length());
            if (StringUtils.isEmpty(tokenId))
            {
                continue;
            }
            try
            {
                Object cached = redisService.getCacheObject(key);
                Long uid = extractLocalUserId(cached);
                if (localUserId.equals(uid))
                {
                    log.info("挤下线 VPN 用户会话 userId={} tokenId={}", localUserId, tokenId);
                    invalidateToken(tokenId);
                }
            }
            catch (Exception e)
            {
                log.warn("扫描并清理 VPN 会话失败 key={}", key, e);
            }
        }
    }

    /**
     * 记录当前有效 token（须在 createToken 之后调用）
     */
    public void bindSession(Long localUserId, String tokenId)
    {
        if (localUserId == null || StringUtils.isEmpty(tokenId))
        {
            return;
        }
        VpnSessionIndex index = new VpnSessionIndex();
        index.setTokenId(tokenId);
        redisService.setCacheObject(userTokenKey(localUserId), index, CacheConstants.EXPIRATION, TimeUnit.MINUTES);
    }

    /**
     * 作废指定 token 及在线登记
     */
    public void invalidateToken(String tokenId)
    {
        if (StringUtils.isEmpty(tokenId))
        {
            return;
        }
        redisService.deleteObject(CacheConstants.LOGIN_TOKEN_KEY + tokenId);
        redisService.deleteObject(CacheConstants.VPN_ONLINE_KEY + tokenId);
    }

    /**
     * 正常登出：仅当索引仍指向该 token 时删除
     */
    public void unbindIfMatch(Long localUserId, String tokenId)
    {
        if (localUserId == null || StringUtils.isEmpty(tokenId))
        {
            return;
        }
        String currentTokenId = readActiveTokenId(localUserId);
        if (tokenId.equals(currentTokenId))
        {
            redisService.deleteObject(userTokenKey(localUserId));
        }
    }

    /**
     * 根据 access_token 解析登出时的 userId 与 tokenId 并解绑
     */
    public void unbindByAccessToken(String accessToken)
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
        try
        {
            Long userId = Long.parseLong(JwtUtils.getUserId(accessToken));
            unbindIfMatch(userId, tokenId);
        }
        catch (Exception ignored)
        {
            // JWT 解析失败时仅跳过索引清理
        }
    }

    /**
     * 心跳等活跃操作时续期单会话索引 TTL（仅当索引仍指向当前 token）
     */
    public void touchSessionIndexByAccessToken(String accessToken)
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
        try
        {
            Long userId = Long.parseLong(JwtUtils.getUserId(accessToken));
            touchSessionIndexIfMatch(userId, tokenId);
        }
        catch (Exception ignored)
        {
            // JWT 解析失败时跳过续期
        }
    }

    /**
     * 续期单会话索引（仅当索引仍指向当前 token）
     */
    public void touchSessionIndexIfMatch(Long localUserId, String tokenId)
    {
        if (localUserId == null || StringUtils.isEmpty(tokenId))
        {
            return;
        }
        String currentTokenId = readActiveTokenId(localUserId);
        if (tokenId.equals(currentTokenId))
        {
            bindSession(localUserId, tokenId);
        }
    }

    /**
     * 会话失效时的提示文案（区分被挤下线与自然过期）
     */
    public String resolveSessionInvalidMessage(String accessToken)
    {
        if (StringUtils.isEmpty(accessToken))
        {
            return "登录状态已过期";
        }
        String tokenId = JwtUtils.getUserKey(accessToken);
        if (StringUtils.isEmpty(tokenId))
        {
            return "登录状态已过期";
        }
        try
        {
            Long userId = Long.parseLong(JwtUtils.getUserId(accessToken));
            String activeTokenId = readActiveTokenId(userId);
            if (StringUtils.isNotEmpty(activeTokenId) && !activeTokenId.equals(tokenId))
            {
                return KICKED_MSG;
            }
            if (StringUtils.isEmpty(activeTokenId) && hasOtherActiveSession(userId, tokenId))
            {
                return KICKED_MSG;
            }
        }
        catch (Exception ignored)
        {
            // 无法解析 JWT 时回退通用文案
        }
        return "登录状态已过期";
    }

    private boolean hasOtherActiveSession(Long localUserId, String currentTokenId)
    {
        Collection<String> keys = redisService.keys(CacheConstants.LOGIN_TOKEN_KEY + "*");
        if (keys == null || keys.isEmpty())
        {
            return false;
        }
        String prefix = CacheConstants.LOGIN_TOKEN_KEY;
        for (String key : keys)
        {
            if (StringUtils.isEmpty(key) || !key.startsWith(prefix))
            {
                continue;
            }
            String tokenId = key.substring(prefix.length());
            if (StringUtils.isEmpty(tokenId) || currentTokenId.equals(tokenId))
            {
                continue;
            }
            Object cached = redisService.getCacheObject(key);
            if (localUserId.equals(extractLocalUserId(cached)))
            {
                return true;
            }
        }
        return false;
    }

    private Long extractLocalUserId(Object cached)
    {
        if (cached == null)
        {
            return null;
        }
        if (cached instanceof VpnLoginUser)
        {
            return resolveVpnLocalUserId((VpnLoginUser) cached);
        }
        try
        {
            JSONObject json = JSON.parseObject(JSON.toJSONString(cached));
            if (json == null || json.isEmpty())
            {
                return null;
            }
            if (json.containsKey("sysUser") && !json.containsKey("vpnUser"))
            {
                return null;
            }
            Long userid = json.getLong("userid");
            if (userid != null)
            {
                return userid;
            }
            JSONObject vpnUser = json.getJSONObject("vpnUser");
            if (vpnUser != null)
            {
                return vpnUser.getLong("userId");
            }
        }
        catch (Exception ignored)
        {
            // 非 VPN 登录缓存结构
        }
        return null;
    }

    private Long resolveVpnLocalUserId(VpnLoginUser loginUser)
    {
        if (loginUser.getUserid() != null)
        {
            return loginUser.getUserid();
        }
        if (loginUser.getVpnUser() != null && loginUser.getVpnUser().getUserId() != null)
        {
            return loginUser.getVpnUser().getUserId();
        }
        return null;
    }

    private String readActiveTokenId(Long localUserId)
    {
        if (localUserId == null)
        {
            return null;
        }
        Object value = redisService.getCacheObject(userTokenKey(localUserId));
        if (value instanceof VpnSessionIndex)
        {
            return normalizeTokenId(((VpnSessionIndex) value).getTokenId());
        }
        if (value instanceof String)
        {
            return normalizeTokenId((String) value);
        }
        try
        {
            JSONObject json = JSON.parseObject(JSON.toJSONString(value));
            if (json != null)
            {
                String tokenId = json.getString("tokenId");
                if (StringUtils.isNotEmpty(tokenId))
                {
                    return normalizeTokenId(tokenId);
                }
            }
        }
        catch (Exception ignored)
        {
            // 兼容历史索引格式
        }
        return null;
    }

    private String normalizeTokenId(String tokenId)
    {
        if (StringUtils.isEmpty(tokenId))
        {
            return null;
        }
        return tokenId.trim();
    }

    private String userTokenKey(Long localUserId)
    {
        return CacheConstants.VPN_USER_TOKEN_KEY + localUserId;
    }
}
