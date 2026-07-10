package com.ruoyi.yianlian.service.impl;

import com.ruoyi.common.core.exception.yianlian.YiAnLianException;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.client.dto.YiAnLianUserAuthRequest;
import com.ruoyi.yianlian.domain.VpnService;
import com.ruoyi.yianlian.domain.VpnServiceGroup;
import com.ruoyi.yianlian.domain.VpnUserYianlianMapping;
import com.ruoyi.yianlian.domain.YalUserAuth;
import com.ruoyi.yianlian.domain.vo.ServiceTreeSelect;
import com.ruoyi.yianlian.mapper.VpnServiceGroupMapper;
import com.ruoyi.yianlian.mapper.VpnServiceMapper;
import com.ruoyi.yianlian.mapper.VpnUserYianlianMappingMapper;
import com.ruoyi.yianlian.mapper.YalUserAuthMapper;
import com.ruoyi.yianlian.domain.VpnUser;
import com.ruoyi.yianlian.service.IYalUserAuthService;
import com.ruoyi.yianlian.service.vpn.IVpnUserService;
import com.ruoyi.yianlian.service.yianlian.IYiAnLianAuthorityService;
import com.ruoyi.common.core.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户授权 服务层实现
 *
 * @author ruoyi
 */
@Service
@Slf4j
public class YalUserAuthServiceImpl implements IYalUserAuthService
{
    @Autowired
    private YalUserAuthMapper yalUserAuthMapper;

    @Autowired
    private VpnServiceGroupMapper serviceGroupMapper;

    @Autowired
    private VpnServiceMapper serviceMapper;

    @Autowired
    private IYiAnLianAuthorityService yiAnLianAuthorityService;

    @Autowired
    private VpnUserYianlianMappingMapper userYianlianMappingMapper;

    @Autowired
    private IVpnUserService vpnUserService;

    @Override
    public YalUserAuth selectYalUserAuthById(Long id)
    {
        return yalUserAuthMapper.selectYalUserAuthById(id);
    }

    @Override
    public List<YalUserAuth> selectYalUserAuthList(YalUserAuth yalUserAuth)
    {
        return yalUserAuthMapper.selectYalUserAuthList(yalUserAuth);
    }

    @Override
    public List<YalUserAuth> selectByUserId(Long userId)
    {
        return yalUserAuthMapper.selectYalUserAuthByUserId(userId);
    }

    @Override
    public List<YalUserAuth> selectByUserIdAndLineId(Long userId, String lineId)
    {
        return yalUserAuthMapper.selectYalUserAuthByUserIdAndLineId(userId, lineId);
    }

    @Override
    public int insertYalUserAuth(YalUserAuth yalUserAuth)
    {
        yalUserAuth.setCreateTime(DateUtils.getNowDate());
        return yalUserAuthMapper.insertYalUserAuth(yalUserAuth);
    }

    @Override
    public int updateYalUserAuth(YalUserAuth yalUserAuth)
    {
        yalUserAuth.setUpdateTime(DateUtils.getNowDate());
        return yalUserAuthMapper.updateYalUserAuth(yalUserAuth);
    }

    @Override
    public int deleteYalUserAuthByIds(Long[] ids)
    {
        return yalUserAuthMapper.deleteYalUserAuthByIds(ids);
    }

    @Override
    public int deleteYalUserAuthById(Long id)
    {
        return yalUserAuthMapper.deleteYalUserAuthById(id);
    }

    /**
     * 批量保存用户授权（先删后插），并同步给易安联
     *
     * @param userId   用户ID
     * @param authList 授权列表
     * @return 结果
     */
    @Override
    @Transactional
    public int batchSaveUserAuth(Long userId, String lineId, List<YalUserAuth> authList)
    {
        if (StringUtils.isEmpty(lineId))
        {
            throw new ServiceException("线路不能为空");
        }
        VpnUser user = vpnUserService.selectUserById(userId);
        if (user == null)
        {
            throw new ServiceException("用户不存在");
        }
        if (StringUtils.isNotEmpty(user.getAppId()) && !lineId.equals(user.getAppId()))
        {
            throw new ServiceException("授权线路与用户所属线路不一致");
        }

        yalUserAuthMapper.deleteYalUserAuthByUserIdAndLineId(userId, lineId);

        int result = 0;
        if (authList != null && !authList.isEmpty())
        {
            for (YalUserAuth auth : authList)
            {
                auth.setUserId(userId);
                auth.setLineId(lineId);
                auth.setCreateTime(DateUtils.getNowDate());
            }
            result = yalUserAuthMapper.batchInsertYalUserAuth(authList);
        }

        // 远程同步失败将抛出异常，触发本地事务回滚（远程失败则本地不变更）
        syncToYiAnLian(userId, authList);

        return result;
    }

    /**
     * 同步用户授权到易安联
     */
    private void syncToYiAnLian(Long userId, List<YalUserAuth> authList)
    {
     if (authList == null || authList.isEmpty())
     {
            return;
        }

        for (YalUserAuth auth : authList)
        {
            String appId = auth.getLineId();
          if (StringUtils.isEmpty(appId))
            {
                continue;
            }

            // 查询该用户在当前线路的易安联映射ID
          VpnUserYianlianMapping mapping = userYianlianMappingMapper.selectByUserIdAndAppId(userId, appId);
            if (mapping == null || StringUtils.isEmpty(mapping.getYianlianId()))
            {
             log.warn("未找到用户[{}]在线路[{}]的易安联映射，跳过同步", userId, appId);
                continue;
          }
            String yianlianUserId = mapping.getYianlianId();
            List<YiAnLianUserAuthRequest> requestList = new ArrayList<>();

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
                         YiAnLianUserAuthRequest req = new YiAnLianUserAuthRequest();
                     req.setUserId(yianlianUserId);
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
                    YiAnLianUserAuthRequest req = new YiAnLianUserAuthRequest();
                          req.setUserId(yianlianUserId);
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
                Boolean success = yiAnLianAuthorityService.grantUserAuthority(appId, requestList);
                if (!Boolean.TRUE.equals(success))
                {
                    throw new YiAnLianException("同步用户授权到易安联线路[" + appId + "]失败");
                }
           else
             {
                    log.info("同步用户[{}]授权到易安联线路[{}]成功，共{}条", userId, appId, requestList.size());
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
