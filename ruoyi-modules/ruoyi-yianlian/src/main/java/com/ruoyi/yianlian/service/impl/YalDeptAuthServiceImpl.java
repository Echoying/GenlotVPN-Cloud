package com.ruoyi.yianlian.service.impl;

import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.exception.yianlian.YiAnLianException;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.domain.VpnDept;
import com.ruoyi.yianlian.service.vpn.IVpnDeptService;
import com.ruoyi.yianlian.client.dto.YiAnLianGroupAuthRequest;
import com.ruoyi.yianlian.domain.VpnDeptYianlianMapping;
import com.ruoyi.yianlian.domain.VpnService;
import com.ruoyi.yianlian.domain.VpnServiceGroup;
import com.ruoyi.yianlian.domain.YalDeptAuth;
import com.ruoyi.yianlian.domain.vo.ServiceTreeSelect;
import com.ruoyi.yianlian.mapper.VpnDeptYianlianMappingMapper;
import com.ruoyi.yianlian.mapper.VpnServiceGroupMapper;
import com.ruoyi.yianlian.mapper.VpnServiceMapper;
import com.ruoyi.yianlian.mapper.YalDeptAuthMapper;
import com.ruoyi.yianlian.service.IYalDeptAuthService;
import com.ruoyi.yianlian.service.vpn.IVpnLineAppService;
import com.ruoyi.yianlian.service.yianlian.IYiAnLianAuthorityService;
import com.ruoyi.yianlian.domain.LineApp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门授权 服务层实现
 *
 * @author ruoyi
 */
@Service
@Slf4j
public class YalDeptAuthServiceImpl implements IYalDeptAuthService
{
    @Autowired
    private YalDeptAuthMapper yalDeptAuthMapper;

    @Autowired
    private VpnServiceGroupMapper serviceGroupMapper;

    @Autowired
    private VpnServiceMapper serviceMapper;

    @Autowired
    private IVpnLineAppService lineAppService;

    @Autowired
    private IYiAnLianAuthorityService yiAnLianAuthorityService;

    @Autowired
    private VpnDeptYianlianMappingMapper deptYianlianMappingMapper;

    @Autowired
    private IVpnDeptService vpnDeptService;

    @Override
    public YalDeptAuth selectYalDeptAuthById(Long id)
    {
        return yalDeptAuthMapper.selectYalDeptAuthById(id);
    }

    @Override
    public List<YalDeptAuth> selectYalDeptAuthList(YalDeptAuth yalDeptAuth)
    {
        return yalDeptAuthMapper.selectYalDeptAuthList(yalDeptAuth);
    }

    @Override
    public List<YalDeptAuth> selectByDeptId(Long deptId)
    {
        return yalDeptAuthMapper.selectYalDeptAuthByDeptId(deptId);
    }

    @Override
    public List<YalDeptAuth> selectByDeptIdAndLineId(Long deptId, String lineId)
    {
        return yalDeptAuthMapper.selectYalDeptAuthByDeptIdAndLineId(deptId, lineId);
    }

    @Override
    public int insertYalDeptAuth(YalDeptAuth yalDeptAuth)
    {
        yalDeptAuth.setCreateTime(DateUtils.getNowDate());
        return yalDeptAuthMapper.insertYalDeptAuth(yalDeptAuth);
    }

    @Override
    public int updateYalDeptAuth(YalDeptAuth yalDeptAuth)
    {
        yalDeptAuth.setUpdateTime(DateUtils.getNowDate());
        return yalDeptAuthMapper.updateYalDeptAuth(yalDeptAuth);
    }

    @Override
    public int deleteYalDeptAuthByIds(Long[] ids)
    {
        return yalDeptAuthMapper.deleteYalDeptAuthByIds(ids);
    }

    @Override
    public int deleteYalDeptAuthById(Long id)
    {
        return yalDeptAuthMapper.deleteYalDeptAuthById(id);
    }

    /**
     * 批量保存部门授权（先删后插），并同步给易安联
     *
     * @param deptId 部门ID
     * @param authList 授权列表
     * @return 结果
     */
    @Override
    @Transactional
    public int batchSaveDeptAuth(Long deptId, String lineId, List<YalDeptAuth> authList)
    {
        if (StringUtils.isEmpty(lineId))
        {
            throw new ServiceException("线路不能为空");
        }
        VpnDept dept = vpnDeptService.selectDeptById(deptId);
        if (dept == null)
        {
            throw new ServiceException("部门不存在");
        }
        if (StringUtils.isNotEmpty(dept.getAppId()) && !lineId.equals(dept.getAppId()))
        {
            throw new ServiceException("授权线路与部门所属线路不一致");
        }

        yalDeptAuthMapper.deleteYalDeptAuthByDeptIdAndLineId(deptId, lineId);

        int result = 0;
        if (authList != null && !authList.isEmpty())
        {
            for (YalDeptAuth auth : authList)
            {
                auth.setDeptId(deptId);
                auth.setLineId(lineId);
                auth.setCreateTime(DateUtils.getNowDate());
            }
            result = yalDeptAuthMapper.batchInsertYalDeptAuth(authList);
        }

        // 远程同步失败将抛出异常，触发本地事务回滚（远程失败则本地不变更）
        syncToYiAnLian(deptId, authList);

        return result;
    }

    /**
     * 同步部门授权到易安联
     * 对每条授权记录（对应一条线路），通过该线路的部门映射ID调用 grantGroupAuthority
     */
    private void syncToYiAnLian(Long deptId, List<YalDeptAuth> authList)
    {
        if (authList == null || authList.isEmpty())
        {
            return;
        }

        for (YalDeptAuth auth : authList)
        {
            String appId = auth.getLineId();
            if (StringUtils.isEmpty(appId))
            {
                continue;
            }

            // 查询该部门在当前线路的易安联映射ID
            VpnDeptYianlianMapping mapping = deptYianlianMappingMapper.selectByDeptIdAndAppId(deptId, appId);
            if (mapping == null || StringUtils.isEmpty(mapping.getYianlianId()))
            {
                log.warn("未找到部门[{}]在线路[{}]的易安联映射，跳过同步", deptId, appId);
                continue;
            }
            String yianlianGroupId = mapping.getYianlianId();

            List<YiAnLianGroupAuthRequest> requestList = new ArrayList<>();

            // 同步应用组授权（使用易安联yianlianKey，而非本地ID）
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
                            req.setGroupId(yianlianGroupId);
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

            // 同步应用授权（使用易安联yianlianKey，而非本地ID）
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
                            req.setGroupId(yianlianGroupId);
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
                    throw new YiAnLianException("同步部门授权到易安联线路[" + appId + "]失败");
                }
                else
                {
                    log.info("同步部门[{}]授权到易安联线路[{}]成功，共{}条", deptId, appId, requestList.size());
                }
            }
        }
    }

    /**
     * 构建应用服务树（从本地数据库查询，应用组为父节点，应用为子节点）
     * 应用组可选，应用可选；没有应用的应用组只展示名称但无法选中（disabled=true）
     *
     * @param appId 线路appId
     * @return 树结构
     */
    @Override
    public List<ServiceTreeSelect> buildServiceTree(String appId)
    {
        // 查询该线路下的所有应用组（平铺列表）
        VpnServiceGroup groupQuery = new VpnServiceGroup();
        if (StringUtils.isNotEmpty(appId))
        {
            groupQuery.setAppId(appId);
        }
        List<VpnServiceGroup> allGroups = serviceGroupMapper.selectServiceGroupList(groupQuery);

        // 查询该线路下的所有应用
        VpnService serviceQuery = new VpnService();
        if (StringUtils.isNotEmpty(appId))
        {
            serviceQuery.setAppId(appId);
        }
        List<VpnService> allServices = serviceMapper.selectServiceList(serviceQuery);

        // 按应用组ID分组，建立 groupId -> List<VpnService> 的映射
        Map<Long, List<VpnService>> servicesByGroup = allServices.stream()
                .collect(Collectors.groupingBy(VpnService::getServiceGroupId));

        // 将所有应用组转换为 ServiceTreeSelect，并挂载子应用
        // 先建立 id -> ServiceTreeSelect 映射
        Map<Long, ServiceTreeSelect> nodeMap = new java.util.LinkedHashMap<>();
        for (VpnServiceGroup group : allGroups)
        {
            List<ServiceTreeSelect> children = new ArrayList<>();

            // 挂载该组下的应用
            List<VpnService> services = servicesByGroup.get(group.getId());
            if (services != null && !services.isEmpty())
            {
                for (VpnService svc : services)
                {
                    children.add(new ServiceTreeSelect(svc));
                }
            }

            ServiceTreeSelect node = new ServiceTreeSelect(group, children.isEmpty() ? null : children);
            // 没有子应用且没有子应用组时，disabled=true（让前端禁止选择，只作展示）
            // 注意：应用组本身可选（即使没有子应用），这里不禁用
            nodeMap.put(group.getId(), node);
        }

        // 构建树形结构：找出根节点（parentId 不在 allGroups 中，或 parentId == 0）
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
                // 挂载到父节点的 children 中
                ServiceTreeSelect parentNode = nodeMap.get(parentId);
                if (parentNode != null)
                {
                    if (parentNode.getChildren() == null)
                    {
                        parentNode.setChildren(new ArrayList<>());
                    }
                    // 将应用组子节点插到子应用前面
                    parentNode.getChildren().add(0, node);
                }
            }
        }

        return rootList;
    }
}
