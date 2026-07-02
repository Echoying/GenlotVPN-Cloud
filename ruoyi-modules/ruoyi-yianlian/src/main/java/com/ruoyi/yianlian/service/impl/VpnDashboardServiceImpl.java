package com.ruoyi.yianlian.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.constant.CacheConstants;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.yianlian.api.domain.VpnUserOnline;
import com.ruoyi.yianlian.api.model.VpnLoginUser;
import com.ruoyi.yianlian.domain.VpnLogininfor;
import com.ruoyi.yianlian.domain.vo.VpnDashboardDistributionsVO;
import com.ruoyi.yianlian.domain.vo.VpnDashboardLoginTrendVO;
import com.ruoyi.yianlian.domain.vo.VpnDashboardNameValueVO;
import com.ruoyi.yianlian.domain.vo.VpnDashboardOverviewVO;
import com.ruoyi.yianlian.domain.vo.VpnDashboardTrendItemVO;
import com.ruoyi.yianlian.mapper.VpnDashboardMapper;
import com.ruoyi.yianlian.service.IVpnDashboardService;

/**
 * VPN 仪表盘 服务实现
 */
@Service
public class VpnDashboardServiceImpl implements IVpnDashboardService
{
    private static final int DEFAULT_DISTRIBUTION_DAYS = 7;

    @Autowired
    private VpnDashboardMapper vpnDashboardMapper;

    @Autowired
    private RedisService redisService;

    @Override
    public VpnDashboardOverviewVO getOverview()
    {
        List<VpnUserOnline> onlineList = loadAllOnlineSessions();
        long onlineTotal = onlineList.size();
        long vpnTokenTotal = countVpnLoginTokens();

        Date todayBegin = DateUtils.parseDate(DateUtils.getDate());
        Date tomorrowBegin = addDays(todayBegin, 1);
        Date yesterdayBegin = addDays(todayBegin, -1);

        VpnDashboardOverviewVO vo = new VpnDashboardOverviewVO();
        vo.setOnlineTotal(onlineTotal);
        vo.setTodayConnectSuccess(vpnDashboardMapper.countConnectSuccess(todayBegin, tomorrowBegin));
        vo.setTodayLoginFail(vpnDashboardMapper.countLoginFail(todayBegin, tomorrowBegin));
        vo.setYesterdayConnectSuccess(vpnDashboardMapper.countConnectSuccess(yesterdayBegin, todayBegin));
        vo.setYesterdayLoginFail(vpnDashboardMapper.countLoginFail(yesterdayBegin, todayBegin));
        vo.setLocalUserTotal(vpnDashboardMapper.countLocalUserTotal());
        vo.setLineEnabledTotal(vpnDashboardMapper.countLineEnabledTotal());
        vo.setAuthenticatedNotConnected(Math.max(0, vpnTokenTotal - onlineTotal));
        return vo;
    }

    @Override
    public VpnDashboardLoginTrendVO getLoginTrend(int days)
    {
        int rangeDays = normalizeDays(days);
        Date beginTime = addDays(DateUtils.parseDate(DateUtils.getDate()), -(rangeDays - 1));
        List<VpnDashboardTrendItemVO> items = vpnDashboardMapper.selectLoginTrend(beginTime);

        Map<String, VpnDashboardTrendItemVO> itemMap = new HashMap<>();
        for (VpnDashboardTrendItemVO item : items)
        {
            if (item.getStatDate() != null)
            {
                itemMap.put(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, item.getStatDate()), item);
            }
        }

        VpnDashboardLoginTrendVO vo = new VpnDashboardLoginTrendVO();
        for (int i = 0; i < rangeDays; i++)
        {
            Date day = addDays(beginTime, i);
            String key = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, day);
            String label = DateUtils.parseDateToStr("MM-dd", day);
            VpnDashboardTrendItemVO item = itemMap.get(key);
            vo.getDates().add(label);
            vo.getConnectSuccess().add(item != null && item.getConnectSuccess() != null ? item.getConnectSuccess() : 0L);
            vo.getLoginFail().add(item != null && item.getLoginFail() != null ? item.getLoginFail() : 0L);
        }
        return vo;
    }

    @Override
    public VpnDashboardDistributionsVO getDistributions(int days)
    {
        int rangeDays = normalizeDays(days);
        Date beginTime = addDays(DateUtils.parseDate(DateUtils.getDate()), -(rangeDays - 1));

        VpnDashboardDistributionsVO vo = new VpnDashboardDistributionsVO();
        vo.setOnlineByLine(aggregateOnlineByLine(loadAllOnlineSessions()));
        vo.setClientOs(vpnDashboardMapper.selectClientOsTop(beginTime, 8));
        vo.setFailReasons(vpnDashboardMapper.selectFailReasonTop(beginTime, 5));
        return vo;
    }

    @Override
    public List<VpnLogininfor> getRecentEvents(int limit)
    {
        int size = limit <= 0 ? 10 : Math.min(limit, 50);
        return vpnDashboardMapper.selectRecentEvents(size);
    }

    private List<VpnUserOnline> loadAllOnlineSessions()
    {
        Collection<String> keys = redisService.scanKeys(CacheConstants.VPN_ONLINE_KEY + "*");
        if (keys == null || keys.isEmpty())
        {
            return new ArrayList<>();
        }
        List<VpnUserOnline> list = new ArrayList<>();
        for (String key : keys)
        {
            VpnUserOnline online = redisService.getCacheObject(key);
            if (online != null)
            {
                list.add(online);
            }
        }
        return list;
    }

    private long countVpnLoginTokens()
    {
        Collection<String> keys = redisService.scanKeys(CacheConstants.LOGIN_TOKEN_KEY + "*");
        if (keys == null || keys.isEmpty())
        {
            return 0L;
        }
        long count = 0L;
        for (String key : keys)
        {
            Object cache = redisService.getCacheObject(key);
            if (cache instanceof VpnLoginUser)
            {
                count++;
            }
        }
        return count;
    }

    private List<VpnDashboardNameValueVO> aggregateOnlineByLine(List<VpnUserOnline> onlineList)
    {
        Map<String, Long> counter = new HashMap<>();
        for (VpnUserOnline online : onlineList)
        {
            String name = StringUtils.isNotEmpty(online.getAppName()) ? online.getAppName()
                    : (StringUtils.isNotEmpty(online.getAppId()) ? online.getAppId() : "未知线路");
            counter.put(name, counter.getOrDefault(name, 0L) + 1L);
        }
        List<VpnDashboardNameValueVO> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : counter.entrySet())
        {
            result.add(new VpnDashboardNameValueVO(entry.getKey(), entry.getValue()));
        }
        result.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return result;
    }

    private int normalizeDays(int days)
    {
        if (days <= 0)
        {
            return DEFAULT_DISTRIBUTION_DAYS;
        }
        return Math.min(days, 90);
    }

    private Date addDays(Date date, int amount)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, amount);
        return calendar.getTime();
    }
}
