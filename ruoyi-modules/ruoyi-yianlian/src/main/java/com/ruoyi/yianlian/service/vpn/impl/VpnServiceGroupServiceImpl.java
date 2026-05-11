package com.ruoyi.yianlian.service.vpn.impl;

import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.domain.VpnServiceGroup;
import com.ruoyi.yianlian.domain.vo.TreeSelect;
import com.ruoyi.yianlian.mapper.VpnServiceGroupMapper;
import com.ruoyi.yianlian.service.vpn.IVpnServiceGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * VPN应用组 服务实现
 */
@Service
public class VpnServiceGroupServiceImpl implements IVpnServiceGroupService
{
    @Autowired
    private VpnServiceGroupMapper serviceGroupMapper;

    @Override
    public List<VpnServiceGroup> selectServiceGroupList(VpnServiceGroup serviceGroup)
    {
        return serviceGroupMapper.selectServiceGroupList(serviceGroup);
    }

    @Override
    public VpnServiceGroup selectServiceGroupById(Long id)
    {
        return serviceGroupMapper.selectServiceGroupById(id);
    }

    @Override
    public int insertServiceGroup(VpnServiceGroup serviceGroup)
    {
        VpnServiceGroup parent = serviceGroupMapper.selectServiceGroupById(serviceGroup.getParentId());
        if (parent != null)
        {
       serviceGroup.setAncestors(parent.getAncestors() + "," + serviceGroup.getParentId());
        }
        else
        {
            serviceGroup.setAncestors("0");
        }
        return serviceGroupMapper.insertServiceGroup(serviceGroup);
    }

    @Override
    public int updateServiceGroup(VpnServiceGroup serviceGroup)
    {
        VpnServiceGroup newParent = serviceGroupMapper.selectServiceGroupById(serviceGroup.getParentId());
        VpnServiceGroup old = serviceGroupMapper.selectServiceGroupById(serviceGroup.getId());
        if (StringUtils.isNotNull(newParent) && StringUtils.isNotNull(old))
        {
       String newAncestors = newParent.getAncestors() + "," + newParent.getId();
            String oldAncestors = old.getAncestors();
        serviceGroup.setAncestors(newAncestors);
            updateChildren(serviceGroup.getId(), newAncestors, oldAncestors);
        }
        else if (StringUtils.isNotNull(old))
        {
            serviceGroup.setAncestors("0");
        }
        return serviceGroupMapper.updateServiceGroup(serviceGroup);
    }

    private void updateChildren(Long id, String newAncestors, String oldAncestors)
    {
        List<VpnServiceGroup> children = serviceGroupMapper.selectChildrenById(id);
        for (VpnServiceGroup child : children)
        {
            child.setAncestors(child.getAncestors().replaceFirst(oldAncestors, newAncestors));
        }
        if (children.size() > 0)
        {
            serviceGroupMapper.updateServiceGroupChildren(children);
        }
    }

    @Override
    public int deleteServiceGroupById(Long id)
    {
      return serviceGroupMapper.deleteServiceGroupById(id);
    }

    @Override
    public int deleteServiceGroupByIds(Long[] ids)
    {
        return serviceGroupMapper.deleteServiceGroupByIds(ids);
    }

    @Override
    public boolean hasChild(Long id)
    {
        return serviceGroupMapper.hasChildById(id) > 0;
    }

    @Override
    public List<VpnServiceGroup> buildTree(List<VpnServiceGroup> list)
    {
      List<VpnServiceGroup> returnList = new ArrayList<>();
        List<Long> tempList = list.stream().map(VpnServiceGroup::getId).collect(Collectors.toList());
        for (VpnServiceGroup group : list)
        {
            if (!tempList.contains(group.getParentId()))
          {
                recursionFn(list, group);
            returnList.add(group);
        }
        }
        if (returnList.isEmpty())
        {
            returnList = list;
     }
        return returnList;
    }

    @Override
    public List<TreeSelect> buildTreeSelect(List<VpnServiceGroup> list)
    {
        List<VpnServiceGroup> trees = buildTree(list);
        return trees.stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    private void recursionFn(List<VpnServiceGroup> list, VpnServiceGroup t)
    {
        List<VpnServiceGroup> childList = getChildList(list, t);
        t.setChildren(childList);
        for (VpnServiceGroup child : childList)
        {
            if (hasChild(list, child))
            {
            recursionFn(list, child);
            }
        }
    }

    private List<VpnServiceGroup> getChildList(List<VpnServiceGroup> list, VpnServiceGroup t)
    {
      List<VpnServiceGroup> tlist = new ArrayList<>();
        Iterator<VpnServiceGroup> it = list.iterator();
    while (it.hasNext())
        {
       VpnServiceGroup n = it.next();
         if (StringUtils.isNotNull(n.getParentId()) && n.getParentId().longValue() == t.getId().longValue())
            {
                tlist.add(n);
          }
        }
        return tlist;
    }

    private boolean hasChild(List<VpnServiceGroup> list, VpnServiceGroup t)
    {
        return getChildList(list, t).size() > 0;
    }
}
