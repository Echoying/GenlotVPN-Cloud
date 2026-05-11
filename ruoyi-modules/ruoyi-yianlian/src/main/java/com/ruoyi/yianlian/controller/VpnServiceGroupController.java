package com.ruoyi.yianlian.controller;

import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.client.dto.YiAnLianServiceGroupListResp;
import com.ruoyi.yianlian.client.dto.vo.YiAnLianServiceGroupVO;
import com.ruoyi.yianlian.domain.VpnServiceGroup;
import com.ruoyi.yianlian.service.vpn.IVpnServiceGroupService;
import com.ruoyi.yianlian.service.yianlian.IYiAnLianServiceGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * VPN应用组管理
 */
@RestController
@RequestMapping("/vpn/serviceGroup")
public class VpnServiceGroupController extends BaseController
{
    @Autowired
    private IVpnServiceGroupService serviceGroupService;

    @Autowired
    private IYiAnLianServiceGroupService yiAnLianServiceGroupService;

    /**
     * 获取应用组列表
     */
    @RequiresPermissions("yianlian:serviceGroup:list")
    @GetMapping("/list")
    public AjaxResult list(VpnServiceGroup serviceGroup)
    {
        List<VpnServiceGroup> list = serviceGroupService.selectServiceGroupList(serviceGroup);
        return success(list);
    }

    /**
   * 获取应用组下拉树列表
     */
    @GetMapping("/treeselect")
    public AjaxResult treeselect(VpnServiceGroup serviceGroup)
    {
        List<VpnServiceGroup> list = serviceGroupService.selectServiceGroupList(serviceGroup);
      return success(serviceGroupService.buildTreeSelect(list));
    }

    /**
     * 查询应用组列表（排除节点）
     */
    @RequiresPermissions("yianlian:serviceGroup:list")
    @GetMapping("/list/exclude/{id}")
    public AjaxResult excludeChild(@PathVariable Long id)
    {
        List<VpnServiceGroup> list = serviceGroupService.selectServiceGroupList(new VpnServiceGroup());
        list.removeIf(g -> g.getId().equals(id)
       || (g.getAncestors() != null && g.getAncestors().contains("," + id + ",")));
        return success(list);
    }

    /**
     * 根据ID获取详细信息
     */
    @RequiresPermissions("yianlian:serviceGroup:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(serviceGroupService.selectServiceGroupById(id));
    }

    /**
     * 新增应用组
     */
    @RequiresPermissions("yianlian:serviceGroup:add")
    @Log(title = "VPN应用组管理", businessType = BusinessType.INSERT)
    @PostMapping
  public AjaxResult add(@Validated @RequestBody VpnServiceGroup serviceGroup)
    {
        serviceGroup.setCreateBy(SecurityUtils.getUsername());
        // 先同步到易安联，获取返回的key
        YiAnLianServiceGroupVO vo = new YiAnLianServiceGroupVO();
      vo.setName(serviceGroup.getGroupName());
        vo.setDescription(serviceGroup.getDescription());
        vo.setParentId("0");
        vo.setPath("/" + serviceGroup.getGroupName());
        // 如果有父节点，用父节点本地存的yianlianKey作为parentId
        if (serviceGroup.getParentId() != null && serviceGroup.getParentId() != 0)
      {
            VpnServiceGroup parent = serviceGroupService.selectServiceGroupById(serviceGroup.getParentId());
         if (parent != null && StringUtils.isNotEmpty(parent.getYianlianKey()))
            {
                vo.setParentId(parent.getYianlianKey());
                vo.setPath(parent.getGroupName() + "/" + serviceGroup.getGroupName());
            }
        }
        String yianlianKey = yiAnLianServiceGroupService.create(serviceGroup.getAppId(), vo);
        if (StringUtils.isNotEmpty(yianlianKey))
        {
        serviceGroup.setYianlianKey(yianlianKey);
        }
        else
        {
          logger.error("同步创建应用组到易安联失败, appId: {}, groupName: {}", serviceGroup.getAppId(), serviceGroup.getGroupName());
        }
        // 本地入库
      int ret = serviceGroupService.insertServiceGroup(serviceGroup);
        return toAjax(ret);
    }

    /**
     * 修改应用组
     */
    @RequiresPermissions("yianlian:serviceGroup:edit")
    @Log(title = "VPN应用组管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody VpnServiceGroup serviceGroup)
    {
        if (serviceGroup.getParentId() != null && serviceGroup.getParentId().equals(serviceGroup.getId()))
      {
          return error("修改应用组'" + serviceGroup.getGroupName() + "'失败，上级应用组不能是自己");
        }
        VpnServiceGroup oldGroup = serviceGroupService.selectServiceGroupById(serviceGroup.getId());
        if (oldGroup == null)
        {
            return error("应用组不存在");
        }
        serviceGroup.setUpdateBy(SecurityUtils.getUsername());
        // 同步到易安联
        if (StringUtils.isNotEmpty(oldGroup.getYianlianKey()))
        {
            String appId = serviceGroup.getAppId() != null ? serviceGroup.getAppId() : oldGroup.getAppId();
            YiAnLianServiceGroupVO updateVO = new YiAnLianServiceGroupVO();
       updateVO.setKey(oldGroup.getYianlianKey());
            updateVO.setName(serviceGroup.getGroupName());
            updateVO.setDescription(serviceGroup.getDescription());
            Boolean syncResult = yiAnLianServiceGroupService.update(appId, updateVO);
            if (syncResult == null || !syncResult)
            {
                logger.error("同步更新应用组到易安联失败, appId: {}, key: {}", appId, oldGroup.getYianlianKey());
        }
        }
        return toAjax(serviceGroupService.updateServiceGroup(serviceGroup));
    }

    /**
     * 删除应用组
     */
    @RequiresPermissions("yianlian:serviceGroup:remove")
    @Log(title = "VPN应用组管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        if (serviceGroupService.hasChild(id))
     {
            return warn("存在下级应用组,不允许删除");
        }
        VpnServiceGroup group = serviceGroupService.selectServiceGroupById(id);
     if (group == null)
        {
            return error("应用组不存在");
        }
        // 同步删除易安联
        if (StringUtils.isNotEmpty(group.getYianlianKey()))
        {
            List<String> deleteIds = new ArrayList<>();
            deleteIds.add(group.getYianlianKey());
            Boolean syncResult = yiAnLianServiceGroupService.delete(group.getAppId(), deleteIds);
            if (syncResult == null || !syncResult)
            {
                logger.error("同步删除应用组到易安联失败, appId: {}, key: {}", group.getAppId(), group.getYianlianKey());
            }
        }
        return toAjax(serviceGroupService.deleteServiceGroupById(id));
    }
}
