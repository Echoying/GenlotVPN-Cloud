package com.ruoyi.yianlian.service.sync.impl;

import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.config.SyncProxyProperties;
import com.ruoyi.yianlian.constant.SyncProxyConstants;
import com.ruoyi.yianlian.domain.LineApp;
import com.ruoyi.yianlian.domain.VpnRole;
import com.ruoyi.yianlian.domain.vo.SyncProxyConfigVO;
import com.ruoyi.yianlian.service.sync.ISyncProxyService;
import com.ruoyi.yianlian.service.sync.SyncProxyEndpointResolver;
import com.ruoyi.yianlian.service.vpn.IVpnLineAppService;
import com.ruoyi.yianlian.service.vpn.IVpnRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 同步代理配置
 */
@Service
public class SyncProxyServiceImpl implements ISyncProxyService
{
    @Autowired
    private IVpnLineAppService lineAppService;

    @Autowired
    private IVpnRoleService roleService;

    @Autowired
    private SyncProxyProperties syncProxyProperties;

    @Autowired
    private SyncProxyEndpointResolver endpointResolver;

    @Override
    public SyncProxyConfigVO getSyncProxyConfig(String appId, Long userId)
    {
        if (userId == null)
        {
            throw new ServiceException("用户未登录");
        }
        if (StringUtils.isEmpty(appId))
        {
            throw new ServiceException("线路不能为空");
        }
        if (!hasSyncProxyRole(userId, appId))
        {
            SyncProxyConfigVO vo = new SyncProxyConfigVO();
            vo.setEnabled(false);
            return vo;
        }
        LineApp lineApp = lineAppService.selectLineAppById(appId);
        if (lineApp == null)
        {
            throw new ServiceException("线路不存在");
        }

        SyncProxyConfigVO vo = new SyncProxyConfigVO();
        if (!syncProxyProperties.isEnabled() || !SyncProxyConstants.isLineProxyEnabled(lineApp))
        {
            vo.setEnabled(false);
            return vo;
        }
        if (StringUtils.isEmpty(lineApp.getUrl()))
        {
            throw new ServiceException("线路管理系统 URL 未配置");
        }

        vo.setEnabled(true);
        vo.setUpstreamUrl(lineApp.getUrl().trim());
        vo.setListenHost(endpointResolver.resolveListenHost(lineApp));
        vo.setListenPort(endpointResolver.resolveListenPort(lineApp));
        vo.setPathPrefix(endpointResolver.resolvePathPrefix());
        vo.setAllowedSourceIps(syncProxyProperties.getAllowedSourceIps().stream()
            .filter(StringUtils::isNotEmpty)
            .map(String::trim)
            .collect(Collectors.toList()));
        return vo;
    }

    private boolean hasSyncProxyRole(Long userId, String appId)
    {
        List<VpnRole> roles = roleService.selectUserRolesByUserId(userId);
        if (roles == null || roles.isEmpty())
        {
            return false;
        }
        return roles.stream().anyMatch(role -> SyncProxyConstants.matchesSyncProxyRole(role, appId));
    }
}
