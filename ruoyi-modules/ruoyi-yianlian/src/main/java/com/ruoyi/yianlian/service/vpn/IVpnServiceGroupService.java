package com.ruoyi.yianlian.service.vpn;

import com.ruoyi.yianlian.domain.VpnServiceGroup;
import com.ruoyi.yianlian.domain.vo.TreeSelect;

import java.util.List;

/**
 * VPN应用组 服务层
 */
public interface IVpnServiceGroupService
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
    public boolean hasChild(Long id);

    /**
     * 构建前端所需要树结构
     */
    public List<VpnServiceGroup> buildTree(List<VpnServiceGroup> list);

    /**
     * 构建前端所需要下拉树结构
     */
    public List<TreeSelect> buildTreeSelect(List<VpnServiceGroup> list);
}
