package com.ruoyi.yianlian.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.constant.CacheConstants;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.yianlian.api.domain.VpnUserOnline;
import com.ruoyi.yianlian.service.IVpnUserOnlineService;

/**
 * VPN 在线用户 服务层处理
 */
@Service
public class VpnUserOnlineServiceImpl implements IVpnUserOnlineService
{
    @Autowired
    private RedisService redisService;

    @Override
    public List<VpnUserOnline> selectOnlineList(String ipaddr, String userName, String appIds)
    {
        Collection<String> keys = redisService.keys(CacheConstants.VPN_ONLINE_KEY + "*");
        if (keys == null || keys.isEmpty())
        {
            return Collections.emptyList();
        }

        Set<String> appIdFilter = parseAppIds(appIds);
        List<VpnUserOnline> list = new ArrayList<>();
        for (String key : keys)
        {
            VpnUserOnline online = redisService.getCacheObject(key);
            if (online == null)
            {
                continue;
            }
            if (StringUtils.isNotEmpty(ipaddr) && !StringUtils.equals(ipaddr, online.getIpaddr()))
            {
                continue;
            }
            if (StringUtils.isNotEmpty(userName) && !StringUtils.equals(userName, online.getUserName()))
            {
                continue;
            }
            if (!appIdFilter.isEmpty() && !appIdFilter.contains(StringUtils.trim(online.getAppId())))
            {
                continue;
            }
            list.add(online);
        }

        list.sort(Comparator.comparing(VpnUserOnline::getLoginTime,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return list;
    }

    private Set<String> parseAppIds(String appIds)
    {
        Set<String> set = new HashSet<>();
        if (StringUtils.isEmpty(appIds))
        {
            return set;
        }
        for (String part : appIds.split(","))
        {
            if (StringUtils.isNotEmpty(part))
            {
                set.add(part.trim());
            }
        }
        return set;
    }
}
