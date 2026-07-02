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
import com.ruoyi.yianlian.api.domain.VpnUserOnline;
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
        redisService.deleteObject(CacheConstants.LOGIN_TOKEN_KEY + tokenId);
        redisService.deleteObject(CacheConstants.VPN_ONLINE_KEY + tokenId);
        return success();
    }
}
