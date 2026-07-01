package com.ruoyi.yianlian.controller;

import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.enums.UserStatus;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.InnerAuth;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.api.domain.VpnChangePasswordRequest;
import com.ruoyi.yianlian.api.domain.VpnUserInfo;
import com.ruoyi.yianlian.api.model.VpnLoginUser;
import com.ruoyi.yianlian.domain.VpnLocalUser;
import com.ruoyi.yianlian.domain.VpnUser;
import com.ruoyi.yianlian.domain.vo.VpnLocalUserSyncRequest;
import com.ruoyi.yianlian.domain.vo.VpnOfflineLoginExportRequest;
import com.ruoyi.yianlian.service.vpn.IVpnLocalUserService;
import com.ruoyi.yianlian.service.vpn.IVpnLocalUserSyncService;
import com.ruoyi.yianlian.service.vpn.IVpnOfflineLoginExportService;
import com.ruoyi.yianlian.service.vpn.IVpnRoleService;
import com.ruoyi.yianlian.service.vpn.IVpnUserService;
import com.ruoyi.yianlian.utils.AesUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * VPN本地用户管理
 */
@RestController
@RequestMapping("/vpn/local/user")
public class VpnLocalUserController extends BaseController
{
    @Autowired
    private IVpnLocalUserService localUserService;

    @Autowired
    private IVpnLocalUserSyncService localUserSyncService;

    @Autowired
    private IVpnUserService userService;

    @Autowired
    private IVpnOfflineLoginExportService offlineLoginExportService;

    @Autowired
    private IVpnRoleService roleService;

    @Autowired
    private AesUtils aesUtils;

    @RequiresPermissions("vpn:localUser:list")
    @GetMapping("/list")
    public TableDataInfo list(VpnLocalUser user)
    {
        startPage();
        List<VpnLocalUser> list = localUserService.selectLocalUserList(user);
        return getDataTable(list);
    }

    @RequiresPermissions("vpn:localUser:query")
    @GetMapping(value = {"/", "/{localUserId}"})
    public AjaxResult getInfo(@PathVariable(value = "localUserId", required = false) Long localUserId)
    {
        if (localUserId == null)
        {
            return success();
        }
        return success(localUserService.selectLocalUserById(localUserId));
    }

    @RequiresPermissions("vpn:localUser:add")
    @Log(title = "本地用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody VpnLocalUser user)
    {
        if (!localUserService.checkUserNameUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        String plainPassword = user.getPassword();
        user.setCreateBy(SecurityUtils.getUsername());
        return toAjax(localUserService.insertLocalUser(user, plainPassword));
    }

    @RequiresPermissions("vpn:localUser:edit")
    @Log(title = "本地用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody VpnLocalUser user)
    {
        user.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(localUserService.updateLocalUser(user));
    }

    @RequiresPermissions("vpn:localUser:remove")
    @Log(title = "本地用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{localUserIds}")
    public AjaxResult remove(@PathVariable Long[] localUserIds)
    {
        return toAjax(localUserService.deleteLocalUserByIds(localUserIds));
    }

    @RequiresPermissions("vpn:localUser:resetPwd")
    @Log(title = "本地用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody VpnLocalUser user)
    {
        String plainPassword = user.getPassword();
        user.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(localUserService.resetPwd(user, plainPassword));
    }

    @RequiresPermissions("vpn:localUser:edit")
    @Log(title = "本地用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody VpnLocalUser user)
    {
        user.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(localUserService.updateLocalUserStatus(user));
    }

    @RequiresPermissions("vpn:localUser:sync")
    @Log(title = "本地用户管理", businessType = BusinessType.OTHER)
    @PostMapping("/sync")
    public AjaxResult sync(@Validated @RequestBody VpnLocalUserSyncRequest request)
    {
        localUserSyncService.syncToLine(request);
        return success();
    }

    @RequiresPermissions("vpn:localUser:query")
    @GetMapping("/line-users/{localUserId}")
    public AjaxResult lineUsers(@PathVariable Long localUserId)
    {
        return success(userService.selectUsersByLocalUserId(localUserId));
    }

    @RequiresPermissions("vpn:localUser:offlineLogin")
    @GetMapping("/offline-lines/{localUserId}")
    public AjaxResult offlineLines(@PathVariable Long localUserId)
    {
        return success(offlineLoginExportService.listSelectableLines(localUserId));
    }

    @RequiresPermissions("vpn:localUser:offlineLogin")
    @Log(title = "离线登录导出", businessType = BusinessType.EXPORT)
    @PostMapping("/offline-export")
    public void offlineExport(HttpServletResponse response, @Validated @RequestBody VpnOfflineLoginExportRequest request)
    {
        offlineLoginExportService.exportZip(response, request);
    }

    @InnerAuth
    @GetMapping("/info/{username}")
    public R<VpnLoginUser> getUserInfo(@PathVariable("username") String username,
                                       @RequestHeader(SecurityConstants.FROM_SOURCE) String source)
    {
        VpnLocalUser localUser = localUserService.selectLocalUserByUserName(username);
        if (localUser == null)
        {
            return R.fail("用户不存在");
        }
        VpnUserInfo apiUser = new VpnUserInfo();
        apiUser.setUserId(localUser.getLocalUserId());
        apiUser.setUserName(localUser.getUserName());
        apiUser.setNickName(localUser.getNickName());
        apiUser.setEmail(localUser.getEmail());
        apiUser.setPhonenumber(localUser.getPhonenumber());
        apiUser.setSex(localUser.getSex());
        apiUser.setAvatar(localUser.getAvatar());
        apiUser.setPassword(localUser.getPassword());
        apiUser.setStatus(localUser.getStatus());
        apiUser.setDelFlag(localUser.getDelFlag());
        apiUser.setLoginIp(localUser.getLoginIp());
        apiUser.setLoginDate(localUser.getLoginDate());
        apiUser.setCreateBy(localUser.getCreateBy());
        apiUser.setCreateTime(localUser.getCreateTime());
        apiUser.setUpdateBy(localUser.getUpdateBy());
        apiUser.setUpdateTime(localUser.getUpdateTime());
        apiUser.setRemark(localUser.getRemark());

        VpnLoginUser vpnLoginUser = new VpnLoginUser();
        vpnLoginUser.setVpnUser(apiUser);
        vpnLoginUser.setUserid(localUser.getLocalUserId());
        vpnLoginUser.setUsername(localUser.getUserName());
        Set<String> roleKeys = roleService.resolveLoginRoleKeysForLocalUser(localUser.getLocalUserId());
        if (!roleKeys.isEmpty())
        {
            vpnLoginUser.setRoles(roleKeys);
        }
        return R.ok(vpnLoginUser);
    }

    @InnerAuth
    @PutMapping("/recordlogin")
    public R<Boolean> recordUserLogin(@RequestBody VpnUserInfo vpnUser,
                                      @RequestHeader(SecurityConstants.FROM_SOURCE) String source)
    {
        VpnLocalUser user = new VpnLocalUser();
        user.setLocalUserId(vpnUser.getUserId());
        user.setLoginIp(vpnUser.getLoginIp());
        user.setLoginDate(vpnUser.getLoginDate());
        localUserService.updateLocalUserLogin(user);
        return R.ok(true);
    }

    @InnerAuth
    @GetMapping("/authorized-lines/{localUserId}")
    public R<List<Map<String, Object>>> getAuthorizedLines(@PathVariable("localUserId") Long localUserId,
                                                            @RequestHeader(SecurityConstants.FROM_SOURCE) String source)
    {
        return R.ok(localUserService.getAuthorizedLines(localUserId));
    }

    @InnerAuth
    @GetMapping("/authorized-check")
    public R<Boolean> isAuthorizedForLine(@RequestParam Long localUserId,
                                          @RequestParam String appId,
                                          @RequestHeader(SecurityConstants.FROM_SOURCE) String source)
    {
        return R.ok(localUserService.isAuthorizedForLine(localUserId, appId));
    }

    @InnerAuth
    @GetMapping("/line-credentials")
    public R<Map<String, String>> getLineUserCredentials(@RequestParam Long localUserId,
                                                        @RequestParam String appId,
                                                        @RequestHeader(SecurityConstants.FROM_SOURCE) String source)
    {
        return R.ok(localUserService.getLineUserCredentials(localUserId, appId));
    }

    @InnerAuth
    @PutMapping("/changePassword")
    public R<Boolean> changePassword(@RequestBody VpnChangePasswordRequest request,
                                   @RequestHeader(SecurityConstants.FROM_SOURCE) String source)
    {
        VpnLocalUser localUser = localUserService.selectLocalUserByUserName(request.getUsername());
        if (localUser == null)
        {
            return R.fail("用户不存在");
        }
        if (UserStatus.DELETED.getCode().equals(localUser.getDelFlag()))
        {
            return R.fail("用户已被删除");
        }
        if (UserStatus.DISABLE.getCode().equals(localUser.getStatus()))
        {
            return R.fail("用户已停用");
        }
        if (!SecurityUtils.matchesPassword(request.getOldPassword(), localUser.getPassword()))
        {
            return R.fail("旧密码错误");
        }
        if (SecurityUtils.matchesPassword(request.getNewPassword(), localUser.getPassword()))
        {
            return R.fail("新密码不能与旧密码相同");
        }
        localUserService.changeLocalPassword(localUser, request.getNewPassword());
        return R.ok(true);
    }

    @Log(title = "本地用户管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("vpn:localUser:list")
    @PostMapping("/export")
    public void export(HttpServletResponse response, VpnLocalUser user)
    {
        List<VpnLocalUser> list = localUserService.selectLocalUserList(user);
        ExcelUtil<VpnLocalUser> util = new ExcelUtil<>(VpnLocalUser.class);
        util.exportExcel(response, list, "本地用户数据");
    }
}
