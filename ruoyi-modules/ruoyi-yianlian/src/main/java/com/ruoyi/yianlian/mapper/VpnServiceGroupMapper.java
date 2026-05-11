package com.ruoyi.yianlian.mapper;

import com.ruoyi.yianlian.domain.VpnServiceGroup;

import java.util.List;

/**
 * VPN应用组 数据层
 */
public interface VpnServiceGroupMapper
{
    /**
     * 查询应用组列表
     */
    public List<VpnServiceGroup> selectServiceGroupList(VpnServiceGroup serviceGroup);

    /**
     * 根据ID查询应用组
     */
    public VpnServiceGroup selectServiceGroupById(Long id);

    /**
     * 新增应用组
     */
    public int insertServiceGroup(VpnServiceGroup serviceGroup);

    /**
     * 修改应用组
     */
    public int updateServiceGroup(VpnServiceGroup serviceGroup);

    /**
     * 删除应用组
     */
    public int deleteServiceGroupById(Long id);

    /**
     * 批量删除应用组
     */
    public int deleteServiceGroupByIds(Long[] ids);

    /**
     * 是否存在子节点
     */
    public int hasChildById(Long id);

    /**
     * 查询子节点
     */
    public List<VpnServiceGroup> selectChildrenById(Long id);

    /**
     * 修改子元素关系
     */
    public int updateServiceGroupChildren(List<VpnServiceGroup> children);
}
