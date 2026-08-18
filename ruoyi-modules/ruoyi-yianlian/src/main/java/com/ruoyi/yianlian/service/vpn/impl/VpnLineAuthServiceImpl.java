package com.ruoyi.yianlian.service.vpn.impl;

import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.domain.LineApp;
import com.ruoyi.yianlian.domain.VpnLocalUser;
import com.ruoyi.yianlian.domain.VpnRole;
import com.ruoyi.yianlian.domain.VpnService;
import com.ruoyi.yianlian.domain.VpnServiceGroup;
import com.ruoyi.yianlian.domain.VpnUser;
import com.ruoyi.yianlian.domain.YalDeptAuth;
import com.ruoyi.yianlian.domain.YalRoleAuth;
import com.ruoyi.yianlian.domain.YalUserAuth;
import com.ruoyi.yianlian.domain.vo.VpnLineAuthItemView;
import com.ruoyi.yianlian.domain.vo.VpnLineAuthLineView;
import com.ruoyi.yianlian.domain.vo.VpnLineAuthView;
import com.ruoyi.yianlian.mapper.YalDeptAuthMapper;
import com.ruoyi.yianlian.mapper.YalRoleAuthMapper;
import com.ruoyi.yianlian.mapper.YalUserAuthMapper;
import com.ruoyi.yianlian.service.vpn.IVpnLineAppService;
import com.ruoyi.yianlian.service.vpn.IVpnLineAuthService;
import com.ruoyi.yianlian.service.vpn.IVpnLocalUserService;
import com.ruoyi.yianlian.service.vpn.IVpnRoleService;
import com.ruoyi.yianlian.service.vpn.IVpnServiceGroupService;
import com.ruoyi.yianlian.service.vpn.IVpnServiceService;
import com.ruoyi.yianlian.service.vpn.IVpnUserService;
import com.ruoyi.yianlian.service.vpn.LineAuthMerger;
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

    @Autowired
    private IVpnUserService userService;

    @Autowired
    private IVpnLocalUserService localUserService;

    @Autowired
    private IVpnRoleService roleService;

    @Autowired
    private IVpnServiceService vpnServiceService;

    @Autowired
    private IVpnServiceGroupService vpnServiceGroupService;

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

    @Override
    public VpnLineAuthView buildLineAuthView(Long userId)
    {
        VpnUser user = userService.selectUserById(userId);
        if (user == null)
        {
            throw new ServiceException("用户不存在");
        }
        VpnLineAuthView view = new VpnLineAuthView();
        view.setUserId(user.getUserId());
        view.setUserName(user.getUserName());
        view.setUserType("line");
        view.setLines(toLineViews(collectAuthItems(user)));
        return view;
    }

    @Override
    public VpnLineAuthView buildLocalLineAuthView(Long localUserId)
    {
        VpnLocalUser local = localUserService.selectLocalUserById(localUserId);
        if (local == null)
        {
            throw new ServiceException("用户不存在");
        }
        Map<String, Map<String, LinkedHashSet<String>>> acc = new LinkedHashMap<String, Map<String, LinkedHashSet<String>>>();
        List<VpnUser> lineUsers = userService.selectUsersByLocalUserId(localUserId);
        if (lineUsers != null)
        {
            for (VpnUser lineUser : lineUsers)
            {
                mergeInto(acc, collectAuthItems(lineUser));
            }
        }
        VpnLineAuthView view = new VpnLineAuthView();
        view.setUserId(local.getLocalUserId());
        view.setUserName(local.getUserName());
        view.setUserType("local");
        view.setLines(toLineViews(acc));
        return view;
    }

    private Map<String, Map<String, LinkedHashSet<String>>> collectAuthItems(VpnUser vpnUser)
    {
        Map<String, Map<String, LinkedHashSet<String>>> acc = new LinkedHashMap<String, Map<String, LinkedHashSet<String>>>();
        if (vpnUser == null || vpnUser.getUserId() == null)
        {
            return acc;
        }
        if (vpnUser.getRoles() == null || vpnUser.getRoles().isEmpty())
        {
            vpnUser.setRoles(roleService.selectUserRolesByUserId(vpnUser.getUserId()));
        }
        String userAppId = vpnUser.getAppId();
        if (vpnUser.getDeptId() != null)
        {
            List<YalDeptAuth> deptAuths = yalDeptAuthMapper.selectYalDeptAuthByDeptId(vpnUser.getDeptId());
            for (YalDeptAuth auth : deptAuths)
            {
                absorbAuth(acc, userAppId, auth.getLineId(), auth.getAppGroupIds(), auth.getAppIds(), "部门");
            }
        }
        if (vpnUser.getRoles() != null)
        {
            for (VpnRole role : vpnUser.getRoles())
            {
                if (role == null || role.getRoleId() == null)
                {
                    continue;
                }
                List<YalRoleAuth> roleAuths = yalRoleAuthMapper.selectYalRoleAuthByRoleId(role.getRoleId());
                for (YalRoleAuth auth : roleAuths)
                {
                    absorbAuth(acc, userAppId, auth.getLineId(), auth.getAppGroupIds(), auth.getAppIds(), "角色");
                }
            }
        }
        List<YalUserAuth> userAuths = yalUserAuthMapper.selectYalUserAuthByUserId(vpnUser.getUserId());
        for (YalUserAuth auth : userAuths)
        {
            absorbAuth(acc, userAppId, auth.getLineId(), auth.getAppGroupIds(), auth.getAppIds(), "用户");
        }
        return acc;
    }

    private void absorbAuth(Map<String, Map<String, LinkedHashSet<String>>> acc, String userAppId,
            String lineId, String appGroupIds, String appIds, String source)
    {
        if (lineId == null || !matchUserLine(userAppId, lineId))
        {
            return;
        }
        Map<String, LinkedHashSet<String>> items = acc.get(lineId);
        if (items == null)
        {
            items = new LinkedHashMap<String, LinkedHashSet<String>>();
            acc.put(lineId, items);
        }
        List<String> groups = LineAuthMerger.splitIds(appGroupIds);
        List<String> apps = LineAuthMerger.splitIds(appIds);
        if (apps.isEmpty() && groups.isEmpty())
        {
            LineAuthMerger.mergeSource(items, LineAuthMerger.itemKey("", ""), source);
            return;
        }
        if (apps.isEmpty())
        {
            for (String groupId : groups)
            {
                LineAuthMerger.mergeSource(items, LineAuthMerger.itemKey(groupId, ""), source);
            }
            return;
        }
        for (String appId : apps)
        {
            String groupId = groups.isEmpty() ? "" : groups.get(0);
            LineAuthMerger.mergeSource(items, LineAuthMerger.itemKey(groupId, appId), source);
        }
        if (apps.isEmpty() == false && groups.size() > 1)
        {
            for (int i = 1; i < groups.size(); i++)
            {
                LineAuthMerger.mergeSource(items, LineAuthMerger.itemKey(groups.get(i), ""), source);
            }
        }
    }

    private void mergeInto(Map<String, Map<String, LinkedHashSet<String>>> dest,
            Map<String, Map<String, LinkedHashSet<String>>> src)
    {
        for (Map.Entry<String, Map<String, LinkedHashSet<String>>> lineEntry : src.entrySet())
        {
            Map<String, LinkedHashSet<String>> destItems = dest.get(lineEntry.getKey());
            if (destItems == null)
            {
                destItems = new LinkedHashMap<String, LinkedHashSet<String>>();
                dest.put(lineEntry.getKey(), destItems);
            }
            for (Map.Entry<String, LinkedHashSet<String>> itemEntry : lineEntry.getValue().entrySet())
            {
                for (String source : itemEntry.getValue())
                {
                    LineAuthMerger.mergeSource(destItems, itemEntry.getKey(), source);
                }
            }
        }
    }

    private List<VpnLineAuthLineView> toLineViews(Map<String, Map<String, LinkedHashSet<String>>> acc)
    {
        List<VpnLineAuthLineView> lines = new ArrayList<VpnLineAuthLineView>();
        for (Map.Entry<String, Map<String, LinkedHashSet<String>>> lineEntry : acc.entrySet())
        {
            VpnLineAuthLineView lineView = new VpnLineAuthLineView();
            lineView.setLineId(lineEntry.getKey());
            lineView.setLineName(resolveLineName(lineEntry.getKey()));
            List<VpnLineAuthItemView> items = new ArrayList<VpnLineAuthItemView>();
            for (Map.Entry<String, LinkedHashSet<String>> itemEntry : lineEntry.getValue().entrySet())
            {
                String key = itemEntry.getKey();
                int sep = key.indexOf('|');
                String groupId = sep < 0 ? "" : key.substring(0, sep);
                String appId = sep < 0 ? "" : key.substring(sep + 1);
                VpnLineAuthItemView item = new VpnLineAuthItemView();
                item.setAppGroupId(groupId);
                item.setAppGroupName(resolveGroupName(groupId));
                item.setAppId(appId);
                item.setAppName(resolveServiceName(appId));
                item.setSources(new ArrayList<String>(itemEntry.getValue()));
                items.add(item);
            }
            lineView.setItems(items);
            lines.add(lineView);
        }
        return lines;
    }

    private String resolveLineName(String lineId)
    {
        LineApp line = lineAppService.selectLineAppById(lineId);
        if (line != null && StringUtils.isNotEmpty(line.getAppName()))
        {
            return line.getAppName();
        }
        return lineId;
    }

    private String resolveGroupName(String groupId)
    {
        if (StringUtils.isEmpty(groupId))
        {
            return "";
        }
        try
        {
            VpnServiceGroup group = vpnServiceGroupService.selectServiceGroupById(Long.parseLong(groupId));
            if (group != null && StringUtils.isNotEmpty(group.getGroupName()))
            {
                return group.getGroupName();
            }
        }
        catch (NumberFormatException ignored)
        {
        }
        return groupId;
    }

    private String resolveServiceName(String serviceId)
    {
        if (StringUtils.isEmpty(serviceId))
        {
            return "";
        }
        try
        {
            VpnService service = vpnServiceService.selectServiceById(Long.parseLong(serviceId));
            if (service != null && StringUtils.isNotEmpty(service.getName()))
            {
                return service.getName();
            }
        }
        catch (NumberFormatException ignored)
        {
        }
        return serviceId;
    }
}
