package com.ruoyi.yianlian.controller;

import com.ruoyi.common.core.constant.UserConstants;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.domain.VpnDept;
import com.ruoyi.yianlian.service.vpn.IVpnDeptService;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * VPN部门信息
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/vpn/dept")
public class VpnDeptController extends BaseController
{
    @Autowired
    private IVpnDeptService deptService;

    /**
     * 获取部门列表
     */
    @RequiresPermissions("yianlian:dept:list")
    @GetMapping("/list")
    public AjaxResult list(VpnDept dept)
    {
        if (StringUtils.isEmpty(dept.getAppId()))
        {
            return success(new ArrayList<>());
        }
        List<VpnDept> depts = deptService.selectDeptList(dept);
        return success(depts);
    }

    /**
     * 查询部门列表（排除节点）
     */
    @RequiresPermissions("yianlian:dept:list")
    @GetMapping("/list/exclude/{deptId}")
    public AjaxResult excludeChild(@PathVariable(value = "deptId", required = false) Long deptId,
            @RequestParam String appId)
    {
        VpnDept query = new VpnDept();
        query.setAppId(appId);
        List<VpnDept> depts = deptService.selectDeptList(query);
        depts.removeIf(d -> d.getDeptId().intValue() == deptId || ArrayUtils.contains(StringUtils.split(d.getAncestors(), ","), deptId + ""));
        return success(depts);
    }

    /**
     * 根据部门编号获取详细信息
     */
    @RequiresPermissions("yianlian:dept:query")
    @GetMapping(value = "/{deptId}")
    public AjaxResult getInfo(@PathVariable Long deptId)
    {
        return success(deptService.selectDeptById(deptId));
    }

    /**
     * 获取部门下拉树列表
     */
    @GetMapping("/treeselect")
    public AjaxResult treeselect(VpnDept dept)
    {
        if (StringUtils.isEmpty(dept.getAppId()))
        {
            return success(new ArrayList<>());
        }
        List<VpnDept> depts = deptService.selectDeptList(dept);
        return success(deptService.buildDeptTreeSelect(depts));
    }

    /**
     * 加载对应角色部门列表树
     */
    @GetMapping(value = "/roleDeptTreeselect/{roleId}")
    public AjaxResult roleDeptTreeselect(@PathVariable("roleId") Long roleId, VpnDept dept)
    {
        if (StringUtils.isEmpty(dept.getAppId()))
        {
            dept = new VpnDept();
        }
        List<VpnDept> depts = deptService.selectDeptList(dept);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("checkedKeys", deptService.selectDeptListByRoleId(roleId));
        ajax.put("depts", deptService.buildDeptTreeSelect(depts));
        return ajax;
    }

    /**
     * 新增部门
     */
    @RequiresPermissions("yianlian:dept:add")
    @Log(title = "VPN部门管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody VpnDept dept)
    {
        AjaxResult validate = validateAppId(dept.getAppId());
        if (validate != null)
        {
            return validate;
        }
        if (!deptService.checkDeptNameUnique(dept))
        {
            return error("新增部门'" + dept.getDeptName() + "'失败，部门名称已存在");
        }
        dept.setCreateBy(SecurityUtils.getUsername());
        return toAjax(deptService.insertDeptWithSync(dept));
    }

    /**
     * 修改部门
     */
    @RequiresPermissions("yianlian:dept:edit")
    @Log(title = "VPN部门管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody VpnDept dept)
    {
        AjaxResult validate = validateAppId(dept.getAppId());
        if (validate != null)
        {
            return validate;
        }
        Long deptId = dept.getDeptId();
        if (!deptService.checkDeptNameUnique(dept))
        {
            return error("修改部门'" + dept.getDeptName() + "'失败，部门名称已存在");
        }
        else if (dept.getParentId().equals(deptId))
        {
            return error("修改部门'" + dept.getDeptName() + "'失败，上级部门不能是自己");
        }
        else if (StringUtils.equals(UserConstants.DEPT_DISABLE, dept.getStatus()) && deptService.selectNormalChildrenDeptById(deptId) > 0)
        {
            return error("该部门包含未停用的子部门！");
        }
        if (dept.getParentId() != null && dept.getParentId() != 0L
                && deptService.selectDeptById(dept.getParentId()) == null)
        {
            return error("修改部门'" + dept.getDeptName() + "'失败，上级部门不存在");
        }
        if (deptService.selectDeptById(deptId) == null)
        {
            return error("原部门不存在");
        }
        dept.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(deptService.updateDeptWithSync(dept));
    }

    /**
     * 保存部门排序
     */
    @RequiresPermissions("yianlian:dept:edit")
    @Log(title = "保存VPN部门排序", businessType = BusinessType.UPDATE)
    @PutMapping("/updateSort")
    public AjaxResult updateSort(@RequestBody Map<String, String> params)
    {
        String[] deptIds = params.get("deptIds").split(",");
        String[] orderNums = params.get("orderNums").split(",");
        deptService.updateDeptSort(deptIds, orderNums);
        return success();
    }

    /**
     * 删除部门
     */
    @RequiresPermissions("yianlian:dept:remove")
    @Log(title = "VPN部门管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{deptId}")
    public AjaxResult remove(@PathVariable Long deptId)
    {
        if (deptService.hasChildByDeptId(deptId))
        {
            return warn("存在下级部门,不允许删除");
        }
        if (deptService.checkDeptExistUser(deptId))
        {
            return warn("部门存在用户,不允许删除");
        }
        if (deptService.selectDeptById(deptId) == null)
        {
            return error("部门不存在");
        }
        return toAjax(deptService.deleteDeptWithSync(deptId));
    }

    private AjaxResult validateAppId(String appId)
    {
        if (StringUtils.isEmpty(appId))
        {
            return error("线路不能为空");
        }
        return null;
    }
}
