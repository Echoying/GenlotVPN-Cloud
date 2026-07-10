package com.ruoyi.yianlian.controller;

import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.domain.VpnServiceGroup;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncCommand;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncConstants;
import com.ruoyi.yianlian.service.sync.orchestrator.YiAnLianSyncOrchestrator;
import com.ruoyi.yianlian.service.vpn.IVpnServiceGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    private YiAnLianSyncOrchestrator syncOrchestrator;

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
        AjaxResult rootCheck = validateRootGroupUnique(serviceGroup);
        if (rootCheck != null)
        {
            return rootCheck;
        }
        serviceGroup.setCreateBy(SecurityUtils.getUsername());
        syncOrchestrator.execute(SyncCommand.ofApi(SyncConstants.BIZ_SERVICE_GROUP, SyncConstants.OP_CREATE,
            serviceGroup.getAppId(), null, serviceGroup));
        return success();
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
        AjaxResult rootCheck = validateRootGroupUnique(serviceGroup);
        if (rootCheck != null)
        {
            return rootCheck;
        }
        VpnServiceGroup oldGroup = serviceGroupService.selectServiceGroupById(serviceGroup.getId());
        if (oldGroup == null)
        {
            return error("应用组不存在");
        }
        serviceGroup.setUpdateBy(SecurityUtils.getUsername());
        String appId = serviceGroup.getAppId() != null ? serviceGroup.getAppId() : oldGroup.getAppId();
        syncOrchestrator.execute(SyncCommand.ofApi(SyncConstants.BIZ_SERVICE_GROUP, SyncConstants.OP_UPDATE,
            appId, serviceGroup.getId(), serviceGroup));
        return success();
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
        syncOrchestrator.execute(SyncCommand.ofApi(SyncConstants.BIZ_SERVICE_GROUP, SyncConstants.OP_DELETE,
            group.getAppId(), id, null));
        return success();
    }

    /**
     * 校验同线路下根应用组唯一性
     */
    private AjaxResult validateRootGroupUnique(VpnServiceGroup serviceGroup)
    {
        if (serviceGroup.getParentId() != null && serviceGroup.getParentId() != 0L)
        {
            return null;
        }
        VpnServiceGroup query = new VpnServiceGroup();
        query.setAppId(serviceGroup.getAppId());
        List<VpnServiceGroup> list = serviceGroupService.selectServiceGroupList(query);
        Long selfId = serviceGroup.getId();
        boolean exists = list.stream().anyMatch(g ->
            (g.getParentId() == null || g.getParentId() == 0L)
            && (selfId == null || !selfId.equals(g.getId())));
        return exists ? error("该线路已存在根应用组，不能再选择根应用组") : null;
    }
}
