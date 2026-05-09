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
import com.ruoyi.yianlian.domain.VpnUser;
import com.ruoyi.yianlian.service.vpn.IVpnDeptService;
import com.ruoyi.yianlian.service.vpn.IVpnRoleService;
import com.ruoyi.yianlian.service.vpn.IVpnUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * VPN用户信息
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/vpn/user")
public class VpnUserController extends BaseController
{
    @Autowired
    private IVpnUserService userService;

    @Autowired
    private IVpnRoleService roleService;

    @Autowired
    private IVpnDeptService deptService;

    /**
     * 获取用户列表
     */
    @RequiresPermissions("yianlian:user:list")
    @GetMapping("/list")
    public TableDataInfo list(VpnUser user)
    {
        startPage();
        List<VpnUser> list = userService.selectUserList(user);
        return getDataTable(list);
    }

    @Log(title = "VPN用户管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("yianlian:user:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, VpnUser user)
    {
        List<VpnUser> list = userService.selectUserList(user);
        ExcelUtil<VpnUser> util = new ExcelUtil<VpnUser>(VpnUser.class);
        util.exportExcel(response, list, "VPN用户数据");
    }

    @Log(title = "VPN用户管理", businessType = BusinessType.IMPORT)
    @RequiresPermissions("yianlian:user:import")
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<VpnUser> util = new ExcelUtil<VpnUser>(VpnUser.class);
        List<VpnUser> userList = util.importExcel(file.getInputStream());
        String operName = SecurityUtils.getUsername();
        String message = userService.importUser(userList, updateSupport, operName);
        return success(message);
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) throws IOException
    {
        ExcelUtil<VpnUser> util = new ExcelUtil<VpnUser>(VpnUser.class);
        util.importTemplateExcel(response, "VPN用户数据");
    }

    /**
     * 根据用户编号获取详细信息
     */
    @RequiresPermissions("yianlian:user:query")
    @GetMapping(value = { "/", "/{userId}" })
    public AjaxResult getInfo(@PathVariable(value = "userId", required = false) Long userId)
    {
        if (StringUtils.isNotNull(userId))
        {
            userService.checkUserAllowed(new VpnUser(userId));
        }
        AjaxResult ajax = AjaxResult.success();
        List<VpnRole> roles = roleService.selectRoleAll();
        ajax.put("roles", roles.stream().filter(r -> !r.getRoleId().equals(1L)).collect(Collectors.toList()));
        if (StringUtils.isNotNull(userId))
        {
            VpnUser sysUser = userService.selectUserById(userId);
            ajax.put(AjaxResult.DATA_TAG, sysUser);
            ajax.put("roleIds", sysUser.getRoles().stream().map(VpnRole::getRoleId).collect(Collectors.toList()));
        }
        return ajax;
    }

    /**
     * 新增用户
     */
    @RequiresPermissions("yianlian:user:add")
    @Log(title = "VPN用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody VpnUser user)
    {
        if (!userService.checkUserNameUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        else if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        else if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setCreateBy(SecurityUtils.getUsername());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        return toAjax(userService.insertUser(user));
    }

    /**
     * 修改用户
     */
    @RequiresPermissions("yianlian:user:edit")
    @Log(title = "VPN用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody VpnUser user)
    {
        userService.checkUserAllowed(user);
        if (!userService.checkUserNameUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        else if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        else if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(userService.updateUser(user));
    }

    /**
     * 删除用户
     */
    @RequiresPermissions("yianlian:user:remove")
    @Log(title = "VPN用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable Long[] userIds)
    {
        return toAjax(userService.deleteUserByIds(userIds));
    }

    /**
     * 重置密码
     */
    @RequiresPermissions("yianlian:user:resetPwd")
    @Log(title = "VPN用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody VpnUser user)
    {
        userService.checkUserAllowed(user);
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        user.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(userService.resetPwd(user));
    }

    /**
     * 状态修改
     */
    @RequiresPermissions("yianlian:user:edit")
    @Log(title = "VPN用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody VpnUser user)
    {
        userService.checkUserAllowed(user);
        user.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(userService.updateUserStatus(user));
    }

    /**
     * 根据用户编号获取授权角色
     */
    @RequiresPermissions("yianlian:user:query")
    @GetMapping("/authRole/{userId}")
    public AjaxResult authRole(@PathVariable("userId") Long userId)
    {
        AjaxResult ajax = AjaxResult.success();
        VpnUser user = userService.selectUserById(userId);
        List<VpnRole> roles = roleService.selectRolesByUserId(userId);
        ajax.put("user", user);
        ajax.put("roles", roles.stream().filter(r -> !r.getRoleId().equals(1L)).collect(Collectors.toList()));
        return ajax;
    }

    /**
     * 用户授权角色
     */
    @RequiresPermissions("yianlian:user:edit")
    @Log(title = "VPN用户管理", businessType = BusinessType.GRANT)
    @PutMapping("/authRole")
    public AjaxResult insertAuthRole(Long userId, Long[] roleIds)
    {
        userService.insertUserAuth(userId, roleIds);
        return success();
    }

    /**
     * 获取部门树列表
     */
    @RequiresPermissions("yianlian:user:list")
    @GetMapping("/deptTree")
    public AjaxResult deptTree(com.ruoyi.yianlian.domain.VpnDept dept)
    {
        return success(deptService.selectDeptTreeList(dept));
    }
}
