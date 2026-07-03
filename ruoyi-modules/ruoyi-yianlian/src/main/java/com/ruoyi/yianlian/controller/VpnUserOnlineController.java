package com.ruoyi.yianlian.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.constant.CacheConstants;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.yianlian.api.domain.VpnUserOnline;
import com.ruoyi.yianlian.api.model.VpnLoginUser;
import com.ruoyi.yianlian.service.IVpnUserOnlineService;

/**
 * VPN 在线用户
 */
@RestController
@RequestMapping("/vpn/online")
public class VpnUserOnlineController extends BaseController
{
    @Autowired
    private IVpnUserOnlineService vpnUserOnlineService;

    @Autowired
    private RedisService redisService;

    @RequiresPermissions("vpn:online:list")
    @GetMapping("/list")
    public TableDataInfo list(String ipaddr, String userName, String appIds)
    {
        List<VpnUserOnline> list = vpnUserOnlineService.selectOnlineList(ipaddr, userName, appIds);
        return getDataTable(list);
    }

    /**
     * 强退 VPN 在线用户
     */
    @RequiresPermissions("vpn:online:forceLogout")
    @Log(title = "VPN在线用户", businessType = BusinessType.FORCE)
    @DeleteMapping("/{tokenId}")
    public AjaxResult forceLogout(@PathVariable String tokenId)
    {
        if (StringUtils.isEmpty(tokenId))
        {
            return error("会话编号不能为空");
        }
        VpnUserOnline online = redisService.getCacheObject(CacheConstants.VPN_ONLINE_KEY + tokenId);
        VpnLoginUser loginUser = redisService.getCacheObject(CacheConstants.LOGIN_TOKEN_KEY + tokenId);
        Long userId = resolveForceLogoutUserId(online, loginUser);
        redisService.deleteObject(CacheConstants.LOGIN_TOKEN_KEY + tokenId);
        redisService.deleteObject(CacheConstants.VPN_ONLINE_KEY + tokenId);
        clearUserTokenIndexIfMatch(userId, tokenId);
        return success();
    }

    private Long resolveForceLogoutUserId(VpnUserOnline online, VpnLoginUser loginUser)
    {
        if (online != null && online.getUserId() != null)
        {
            return online.getUserId();
        }
        if (loginUser == null)
        {
            return null;
        }
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

    private void clearUserTokenIndexIfMatch(Long userId, String tokenId)
    {
        if (userId == null || StringUtils.isEmpty(tokenId))
        {
            return;
        }
        String indexKey = CacheConstants.VPN_USER_TOKEN_KEY + userId;
        String currentTokenId = readActiveTokenId(indexKey);
        if (tokenId.equals(currentTokenId))
        {
            redisService.deleteObject(indexKey);
        }
    }

    private String readActiveTokenId(String indexKey)
    {
        Object value = redisService.getCacheObject(indexKey);
        if (value == null)
        {
            return null;
        }
        if (value instanceof String)
        {
            return ((String) value).trim();
        }
        try
        {
            JSONObject json = JSON.parseObject(JSON.toJSONString(value));
            if (json != null)
            {
                String activeTokenId = json.getString("tokenId");
                if (StringUtils.isNotEmpty(activeTokenId))
                {
                    return activeTokenId.trim();
                }
            }
        }
        catch (Exception ignored)
        {
            // 兼容历史索引格式
        }
        return null;
    }
}
