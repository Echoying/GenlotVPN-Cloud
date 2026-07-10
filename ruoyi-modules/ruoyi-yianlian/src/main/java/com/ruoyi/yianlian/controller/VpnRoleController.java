package com.ruoyi.yianlian.controller;

import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.domain.VpnRole;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncCommand;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncConstants;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncDeferredException;
import com.ruoyi.yianlian.service.sync.orchestrator.YiAnLianSyncOrchestrator;
import com.ruoyi.yianlian.service.vpn.IVpnRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 角色信息
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/vpn/role")
public class VpnRoleController extends BaseController {

    @Autowired
    private IVpnRoleService roleService;

    @Autowired
    private YiAnLianSyncOrchestrator syncOrchestrator;

    @RequiresPermissions("yianlian:role:list")
    @GetMapping("/list")
    public TableDataInfo list(VpnRole role) {
        startPage();
        List<VpnRole> list = roleService.selectRoleList(role);
        return getDataTable(list);
    }

    @Log(title = "VPN角色管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("yianlian:role:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, VpnRole role) {
        List<VpnRole> list = roleService.selectRoleList(role);
        ExcelUtil<VpnRole> util = new ExcelUtil<VpnRole>(VpnRole.class);
        util.exportExcel(response, list, "VPN角色数据");
    }

    @RequiresPermissions("yianlian:role:query")
    @GetMapping(value = "/{roleId}")
    public AjaxResult getInfo(@PathVariable Long roleId) {
        return success(roleService.selectRoleById(roleId));
    }

    @RequiresPermissions("yianlian:role:add")
    @Log(title = "VPN角色管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody VpnRole role) {
        AjaxResult validate = validateAppId(role.getAppId());
        if (validate != null) {
            return validate;
        }
        if (!roleService.checkRoleNameUnique(role)) {
            return error("新增角色'" + role.getRoleName() + "'失败，角色名称已存在");
        }
        if (!roleService.checkRoleKeyUnique(role)) {
            return error("新增角色'" + role.getRoleName() + "'失败，角色权限字符已存在");
        }
        role.setCreateBy(SecurityUtils.getUsername());
        syncOrchestrator.execute(SyncCommand.ofApi(SyncConstants.BIZ_ROLE, SyncConstants.OP_CREATE,
            role.getAppId(), null, role));
        return success(role.getRoleId());
    }

    @RequiresPermissions("yianlian:role:edit")
    @Log(title = "VPN角色管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody VpnRole role) {
        AjaxResult validate = validateAppId(role.getAppId());
        if (validate != null) {
            return validate;
        }
        if (!roleService.checkRoleNameUnique(role)) {
            return error("修改角色'" + role.getRoleName() + "'失败，角色名称已存在");
        }
        if (!roleService.checkRoleKeyUnique(role)) {
            return error("修改角色'" + role.getRoleName() + "'失败，角色权限字符已存在");
        }
        role.setUpdateBy(SecurityUtils.getUsername());
        syncOrchestrator.execute(SyncCommand.ofApi(SyncConstants.BIZ_ROLE, SyncConstants.OP_UPDATE,
            role.getAppId(), role.getRoleId(), role));
        return success();
    }

    @RequiresPermissions("yianlian:role:edit")
    @Log(title = "VPN角色管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody VpnRole role) {
        role.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(roleService.updateRoleStatus(role));
    }

    @RequiresPermissions("yianlian:role:remove")
    @Log(title = "VPN角色管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{roleIds}")
    public AjaxResult remove(@PathVariable Long[] roleIds) {
        boolean anyDeferred = false;
        for (Long roleId : roleIds) {
            VpnRole role = roleService.selectRoleById(roleId);
            if (role == null) {
                continue;
            }
            try {
                syncOrchestrator.execute(SyncCommand.ofApi(SyncConstants.BIZ_ROLE, SyncConstants.OP_DELETE,
                    role.getAppId(), roleId, null));
            } catch (SyncDeferredException e) {
                anyDeferred = true;
            }
        }
        if (anyDeferred) {
            throw new SyncDeferredException(null, "部分角色代理暂不可用，已加入补偿队列，稍后自动重试");
        }
        return success();
    }

    @RequiresPermissions("yianlian:role:query")
    @GetMapping("/optionselect")
    public AjaxResult optionselect() {
        return success(roleService.selectRoleAll());
    }

    private AjaxResult validateAppId(String appId) {
        if (StringUtils.isEmpty(appId)) {
            return error("线路不能为空");
        }
        return null;
    }
}
