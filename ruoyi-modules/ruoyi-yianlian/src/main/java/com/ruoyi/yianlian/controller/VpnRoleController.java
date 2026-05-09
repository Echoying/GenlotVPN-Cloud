package com.ruoyi.yianlian.controller;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.domain.VpnDept;
import com.ruoyi.yianlian.domain.VpnRole;
import com.ruoyi.yianlian.service.vpn.IVpnDeptService;
import com.ruoyi.yianlian.service.vpn.IVpnRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * VPN角色信息
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/vpn/role")
public class VpnRoleController extends BaseController
{
    @Autowired
    private IVpnRoleService roleService;

    @Autowired
    private IVpnDeptService deptService;

    @RequiresPermissions("yianlian:role:list")
    @GetMapping("/list")
    public TableDataInfo list(VpnRole role)
    {
        startPage();
        List<VpnRole> list = roleService.selectRoleList(role);
        return getDataTable(list);
    }

    @Log(title = "VPN角色管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("yianlian:role:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, VpnRole role)
    {
        List<VpnRole> list = roleService.selectRoleList(role);
        ExcelUtil<VpnRole> util = new ExcelUtil<VpnRole>(VpnRole.class);
        util.exportExcel(response, list, "VPN角色数据");
    }

    /**
     * 根据角色编号获取详细信息
     */
    @RequiresPermissions("yianlian:role:query")
    @GetMapping(value = "/{roleId}")
    public AjaxResult getInfo(@PathVariable Long roleId)
    {
        return success(roleService.selectRoleById(roleId));
    }

    /**
     * 新增角色
     */
    @RequiresPermissions("yianlian:role:add")
    @Log(title = "VPN角色管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody VpnRole role)
    {
        if (!roleService.checkRoleNameUnique(role))
        {
            return error("新增角色'" + role.getRoleName() + "'失败，角色名称已存在");
        }
        else if (!roleService.checkRoleKeyUnique(role))
        {
            return error("新增角色'" + role.getRoleName() + "'失败，角色权限已存在");
        }
        role.setCreateBy(SecurityUtils.getUsername());
        return toAjax(roleService.insertRole(role));

    }

    /**
     * 修改保存角色
     */
    @RequiresPermissions("yianlian:role:edit")
    @Log(title = "VPN角色管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody VpnRole role)
    {
        if (!roleService.checkRoleNameUnique(role))
        {
            return error("修改角色'" + role.getRoleName() + "'失败，角色名称已存在");
        }
        else if (!roleService.checkRoleKeyUnique(role))
        {
            return error("修改角色'" + role.getRoleName() + "'失败，角色权限已存在");
        }
        role.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(roleService.updateRole(role));
    }

    /**
     * 修改保存数据权限
     */
    @RequiresPermissions("yianlian:role:edit")
    @Log(title = "VPN角色管理", businessType = BusinessType.UPDATE)
    @PutMapping("/dataScope")
    public AjaxResult dataScope(@RequestBody VpnRole role)
    {
        return toAjax(roleService.authDataScope(role));
    }

    /**
     * 状态修改
     */
    @RequiresPermissions("yianlian:role:edit")
    @Log(title = "VPN角色管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody VpnRole role)
    {
        role.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(roleService.updateRoleStatus(role));
    }

    /**
     * 删除角色
     */
    @RequiresPermissions("yianlian:role:remove")
    @Log(title = "VPN角色管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{roleIds}")
    public AjaxResult remove(@PathVariable Long[] roleIds)
    {
        return toAjax(roleService.deleteRoleByIds(roleIds));
    }

    /**
     * 获取角色选择框列表
     */
    @RequiresPermissions("yianlian:role:query")
    @GetMapping("/optionselect")
    public AjaxResult optionselect()
    {
        return success(roleService.selectRoleAll());
    }

    /**
     * 获取对应角色部门树列表
     */
    @RequiresPermissions("yianlian:role:query")
    @GetMapping(value = "/deptTree/{roleId}")
    public AjaxResult deptTree(@PathVariable("roleId") Long roleId)
    {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("checkedKeys", deptService.selectDeptListByRoleId(roleId));
        ajax.put("depts", deptService.selectDeptTreeList(new VpnDept()));
        return ajax;
    }
}
