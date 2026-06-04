package com.ruoyi.yianlian.service.impl;

import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.domain.VpnRole;
import com.ruoyi.yianlian.client.dto.YiAnLianGroupAuthRequest;
import com.ruoyi.yianlian.domain.VpnDeptYianlianMapping;
import com.ruoyi.yianlian.domain.VpnRoleYianlianMapping;
import com.ruoyi.yianlian.domain.VpnService;
import com.ruoyi.yianlian.domain.VpnServiceGroup;
import com.ruoyi.yianlian.domain.YalRoleAuth;
import com.ruoyi.yianlian.domain.vo.ServiceTreeSelect;
import com.ruoyi.yianlian.mapper.VpnRoleYianlianMappingMapper;
import com.ruoyi.yianlian.mapper.VpnServiceGroupMapper;
import com.ruoyi.yianlian.mapper.VpnServiceMapper;
import com.ruoyi.yianlian.mapper.YalRoleAuthMapper;
import com.ruoyi.yianlian.service.IYalRoleAuthService;
import com.ruoyi.yianlian.service.vpn.IVpnRoleService;
import com.ruoyi.yianlian.service.yianlian.IYiAnLianAuthorityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 角色授权 服务层实现
 *
 * @author ruoyi
 */
@Service
@Slf4j
public class YalRoleAuthServiceImpl implements IYalRoleAuthService
{
    @Autowired
    private YalRoleAuthMapper yalRoleAuthMapper;

    @Autowired
    private VpnServiceGroupMapper serviceGroupMapper;

    @Autowired
    private VpnServiceMapper serviceMapper;

    @Autowired
    private IYiAnLianAuthorityService yiAnLianAuthorityService;

    @Autowired
    private VpnRoleYianlianMappingMapper roleYianlianMappingMapper;

    @Autowired
    private IVpnRoleService vpnRoleService;

    @Override
    public YalRoleAuth selectYalRoleAuthById(Long id)
    {
        return yalRoleAuthMapper.selectYalRoleAuthById(id);
    }

    @Override
    public List<YalRoleAuth> selectYalRoleAuthList(YalRoleAuth yalRoleAuth)
    {
        return yalRoleAuthMapper.selectYalRoleAuthList(yalRoleAuth);
    }

    @Override
    public List<YalRoleAuth> selectByRoleId(Long roleId)
    {
        return yalRoleAuthMapper.selectYalRoleAuthByRoleId(roleId);
    }

    @Override
    public List<YalRoleAuth> selectByRoleIdAndLineId(Long roleId, String lineId)
    {
        return yalRoleAuthMapper.selectYalRoleAuthByRoleIdAndLineId(roleId, lineId);
    }

    @Override
    public int insertYalRoleAuth(YalRoleAuth yalRoleAuth)
    {
        yalRoleAuth.setCreateTime(DateUtils.getNowDate());
        return yalRoleAuthMapper.insertYalRoleAuth(yalRoleAuth);
    }

    @Override
    public int updateYalRoleAuth(YalRoleAuth yalRoleAuth)
    {
        yalRoleAuth.setUpdateTime(DateUtils.getNowDate());
        return yalRoleAuthMapper.updateYalRoleAuth(yalRoleAuth);
    }

    @Override
    public int deleteYalRoleAuthByIds(Long[] ids)
    {
        return yalRoleAuthMapper.deleteYalRoleAuthByIds(ids);
    }

    @Override
    public int deleteYalRoleAuthById(Long id)
    {
        return yalRoleAuthMapper.deleteYalRoleAuthById(id);
    }

    /**
     * 批量保存角色授权（先删后插），并同步给易安联
     *
     * @param roleId   角色ID
     * @param authList 授权列表
     * @return 结果
     */
    @Override
    @Transactional
    public int batchSaveRoleAuth(Long roleId, String lineId, List<YalRoleAuth> authList)
    {
        if (StringUtils.isEmpty(lineId))
        {
            throw new ServiceException("线路不能为空");
        }
        VpnRole role = vpnRoleService.selectRoleById(roleId);
        if (role == null)
        {
            throw new ServiceException("角色不存在");
        }
        if (StringUtils.isNotEmpty(role.getAppId()) && !lineId.equals(role.getAppId()))
        {
            throw new ServiceException("授权线路与角色所属线路不一致");
        }

        yalRoleAuthMapper.deleteYalRoleAuthByRoleIdAndLineId(roleId, lineId);

        int result = 0;
        if (authList != null && !authList.isEmpty())
        {
            for (YalRoleAuth auth : authList)
            {
                auth.setRoleId(roleId);
                auth.setLineId(lineId);
                auth.setCreateTime(DateUtils.getNowDate());
            }
            result = yalRoleAuthMapper.batchInsertYalRoleAuth(authList);
        }

        // 同步给易安联
        try
        {
            syncToYiAnLian(roleId, authList);
        }
        catch (Exception e)
        {
            log.error("同步角色授权到易安联失败, roleId: {}, 错误: ", roleId, e);
        }

        return result;
    }

    /**
     * 同步角色授权到易安联
     */
    private void syncToYiAnLian(Long roleId, List<YalRoleAuth> authList)
    {
        if (authList == null || authList.isEmpty())
        {
            return;
        }

        for (YalRoleAuth auth : authList)
        {
            String appId = auth.getLineId();
            if (StringUtils.isEmpty(appId))
            {
                continue;
            }

            // 查询该角色在当前线路的易安联映射ID
            VpnRoleYianlianMapping mapping = roleYianlianMappingMapper.selectByRoleIdAndAppId(roleId, appId);
            if (mapping == null || StringUtils.isEmpty(mapping.getYianlianId()))
            {
                log.warn("未找到角色[{}]在线路[{}]的易安联映射，跳过同步", roleId, appId);
                continue;
            }
            String yianlianGroupId = mapping.getYianlianId();

            List<YiAnLianGroupAuthRequest> requestList = new ArrayList<>();

            // 同步应用组授权
            if (StringUtils.isNotEmpty(auth.getAppGroupIds()))
            {
                String[] groupIds = auth.getAppGroupIds().split(",");
                for (String groupId : groupIds)
                {
                    if (StringUtils.isNotEmpty(groupId.trim()))
                    {
                        try
                        {
                            VpnServiceGroup sg = serviceGroupMapper.selectServiceGroupById(Long.parseLong(groupId.trim()));
                            if (sg == null || StringUtils.isEmpty(sg.getYianlianKey()))
                            {
                                log.warn("应用组[{}]未找到易安联映射Key，跳过", groupId.trim());
                                continue;
                            }
                            YiAnLianGroupAuthRequest req = new YiAnLianGroupAuthRequest();
                            req.setRoleId(yianlianGroupId);
                            req.setServiceGroupId(sg.getYianlianKey());
                            requestList.add(req);
                        }
                        catch (NumberFormatException e)
                        {
                            log.warn("应用组ID格式错误: {}", groupId.trim());
                        }
                    }
                }
            }

            // 同步应用授权
            if (StringUtils.isNotEmpty(auth.getAppIds()))
            {
                String[] serviceIds = auth.getAppIds().split(",");
                for (String serviceId : serviceIds)
                {
                    if (StringUtils.isNotEmpty(serviceId.trim()))
                    {
                        try
                        {
                            VpnService svc = serviceMapper.selectServiceById(Long.parseLong(serviceId.trim()));
                            if (svc == null || StringUtils.isEmpty(svc.getYianlianKey()))
                            {
                                log.warn("应用[{}]未找到易安联映射Key，跳过", serviceId.trim());
                                continue;
                            }
                            YiAnLianGroupAuthRequest req = new YiAnLianGroupAuthRequest();
                                        req.setRoleId(yianlianGroupId);
                            req.setServiceId(svc.getYianlianKey());
                            requestList.add(req);
                        }
                        catch (NumberFormatException e)
                        {
                            log.warn("应用ID格式错误: {}", serviceId.trim());
                        }
                    }
                }
            }

            if (!requestList.isEmpty())
            {
                Boolean success = yiAnLianAuthorityService.grantGroupAuthority(appId, requestList);
                if (!Boolean.TRUE.equals(success))
                {
                    log.warn("同步角色[{}]授权到易安联线路[{}]失败", roleId, appId);
                }
                else
                {
                    log.info("同步角色[{}]授权到易安联线路[{}]成功，共{}条", roleId, appId, requestList.size());
                }
            }
        }
    }

    /**
     * 构建应用服务树（复用部门授权的树结构逻辑）
     *
     * @param appId 线路appId
     * @return 树结构
     */
    @Override
    public List<ServiceTreeSelect> buildServiceTree(String appId)
    {
        VpnServiceGroup groupQuery = new VpnServiceGroup();
        if (StringUtils.isNotEmpty(appId))
        {
            groupQuery.setAppId(appId);
        }
        List<VpnServiceGroup> allGroups = serviceGroupMapper.selectServiceGroupList(groupQuery);

        VpnService serviceQuery = new VpnService();
        if (StringUtils.isNotEmpty(appId))
        {
            serviceQuery.setAppId(appId);
        }
        List<VpnService> allServices = serviceMapper.selectServiceList(serviceQuery);

        Map<Long, List<VpnService>> servicesByGroup = allServices.stream()
                .collect(Collectors.groupingBy(VpnService::getServiceGroupId));

        Map<Long, ServiceTreeSelect> nodeMap = new java.util.LinkedHashMap<>();
        for (VpnServiceGroup group : allGroups)
        {
            List<ServiceTreeSelect> children = new ArrayList<>();
            List<VpnService> services = servicesByGroup.get(group.getId());
            if (services != null && !services.isEmpty())
            {
                for (VpnService svc : services)
                {
                    children.add(new ServiceTreeSelect(svc));
                }
            }
            ServiceTreeSelect node = new ServiceTreeSelect(group, children.isEmpty() ? null : children);
            nodeMap.put(group.getId(), node);
        }

        List<Long> allGroupIds = allGroups.stream().map(VpnServiceGroup::getId).collect(Collectors.toList());
        List<ServiceTreeSelect> rootList = new ArrayList<>();

        for (VpnServiceGroup group : allGroups)
        {
            ServiceTreeSelect node = nodeMap.get(group.getId());
            Long parentId = group.getParentId();
            if (parentId == null || parentId == 0L || !allGroupIds.contains(parentId))
            {
                rootList.add(node);
            }
            else
            {
                ServiceTreeSelect parentNode = nodeMap.get(parentId);
                if (parentNode != null)
                {
                    if (parentNode.getChildren() == null)
                    {
                        parentNode.setChildren(new ArrayList<>());
                    }
                    parentNode.getChildren().add(0, node);
                }
            }
        }

        return rootList;
    }
}
