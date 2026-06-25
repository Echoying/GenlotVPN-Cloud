package com.ruoyi.yianlian.service.vpn.impl;

import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.domain.LineApp;
import com.ruoyi.yianlian.domain.VpnRole;
import com.ruoyi.yianlian.domain.VpnUser;
import com.ruoyi.yianlian.domain.YalDeptAuth;
import com.ruoyi.yianlian.domain.YalRoleAuth;
import com.ruoyi.yianlian.domain.YalUserAuth;
import com.ruoyi.yianlian.mapper.YalDeptAuthMapper;
import com.ruoyi.yianlian.mapper.YalRoleAuthMapper;
import com.ruoyi.yianlian.mapper.YalUserAuthMapper;
import com.ruoyi.yianlian.service.vpn.IVpnLineAppService;
import com.ruoyi.yianlian.service.vpn.IVpnLineAuthService;
import com.ruoyi.yianlian.utils.AesUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * VPN 线路授权服务
 */
@Service
public class VpnLineAuthServiceImpl implements IVpnLineAuthService
{
    @Autowired
    private YalDeptAuthMapper yalDeptAuthMapper;

    @Autowired
    private YalRoleAuthMapper yalRoleAuthMapper;

    @Autowired
    private YalUserAuthMapper yalUserAuthMapper;

    @Autowired
    private IVpnLineAppService lineAppService;

    @Autowired
    private AesUtils aesUtils;

    @Override
    public Set<String> resolveAuthorizedLineIds(VpnUser vpnUser)
    {
        Set<String> lineIdSet = new LinkedHashSet<>();
        if (vpnUser == null || vpnUser.getUserId() == null)
        {
            return lineIdSet;
        }
        String userAppId = vpnUser.getAppId();
        Long userId = vpnUser.getUserId();

        if (vpnUser.getDeptId() != null)
        {
            List<YalDeptAuth> deptAuths = yalDeptAuthMapper.selectYalDeptAuthByDeptId(vpnUser.getDeptId());
            for (YalDeptAuth auth : deptAuths)
            {
                if (auth.getLineId() != null && matchUserLine(userAppId, auth.getLineId()))
                {
                    lineIdSet.add(auth.getLineId());
                }
            }
        }

        if (vpnUser.getRoles() != null)
        {
            for (VpnRole role : vpnUser.getRoles())
            {
                List<YalRoleAuth> roleAuths = yalRoleAuthMapper.selectYalRoleAuthByRoleId(role.getRoleId());
                for (YalRoleAuth auth : roleAuths)
                {
                    if (auth.getLineId() != null && matchUserLine(userAppId, auth.getLineId()))
                    {
                        lineIdSet.add(auth.getLineId());
                    }
                }
            }
        }

        List<YalUserAuth> userAuths = yalUserAuthMapper.selectYalUserAuthByUserId(userId);
        for (YalUserAuth auth : userAuths)
        {
            if (auth.getLineId() != null && matchUserLine(userAppId, auth.getLineId()))
            {
                lineIdSet.add(auth.getLineId());
            }
        }
        return lineIdSet;
    }

    @Override
    public List<Map<String, Object>> toAuthorizedLineVos(Set<String> lineIdSet)
    {
        if (lineIdSet == null || lineIdSet.isEmpty())
        {
            return new ArrayList<>();
        }
        List<LineApp> allLines = lineAppService.selectLineAppList(new LineApp());
        return allLines.stream()
            .filter(line -> lineIdSet.contains(line.getAppId()) && "0".equals(line.getStatus()))
            .map(this::toLineVo)
            .collect(Collectors.toList());
    }

    private boolean matchUserLine(String userAppId, String lineId)
    {
        return StringUtils.isEmpty(userAppId) || userAppId.equals(lineId);
    }

    private Map<String, Object> toLineVo(LineApp line)
    {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("appId", line.getAppId());
        vo.put("appName", line.getAppName());
        vo.put("host", line.getHost());
        vo.put("srvPort", line.getSrvPort());
        vo.put("spaPort", line.getSpaPort());
        String spaKey = line.getSpaKey();
        if (spaKey != null && !spaKey.isEmpty())
        {
            spaKey = AesUtils.md5(aesUtils.decrypt(spaKey));
        }
        vo.put("spaKey", spaKey);
        return vo;
    }
}
