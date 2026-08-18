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
import com.ruoyi.yianlian.domain.VpnDept;
import com.ruoyi.yianlian.constant.SyncProxyConstants;
import com.ruoyi.yianlian.domain.VpnRole;
import com.ruoyi.yianlian.domain.VpnUser;
import com.ruoyi.yianlian.domain.vo.VpnLineBatchSyncRequest;
import com.ruoyi.yianlian.service.sync.handler.payload.AssignRolesPayload;
import com.ruoyi.yianlian.service.sync.handler.payload.ResetPwdPayload;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncCommand;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncConstants;
import com.ruoyi.yianlian.service.sync.orchestrator.SyncDeferredException;
import com.ruoyi.yianlian.service.sync.orchestrator.YiAnLianSyncOrchestrator;
import com.ruoyi.yianlian.service.vpn.IVpnLocalUserSyncService;
import com.ruoyi.yianlian.service.vpn.IVpnDeptService;
import com.ruoyi.yianlian.service.vpn.IVpnLineAuthService;
import com.ruoyi.yianlian.service.vpn.IVpnRoleService;
import com.ruoyi.yianlian.service.vpn.IVpnUserService;
import com.ruoyi.yianlian.utils.AesUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户信息
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/vpn/user")
public class VpnUserController extends BaseController {

    @Autowired
    private IVpnUserService userService;

    @Autowired
    private IVpnRoleService roleService;

    @Autowired
    private IVpnDeptService deptService;

    @Autowired
    private IVpnLineAuthService lineAuthService;

    @Autowired
    private IVpnLocalUserSyncService localUserSyncService;

    @Autowired
    private AesUtils aesUtils;

    @Autowired
    private YiAnLianSyncOrchestrator syncOrchestrator;

    /**
     * 获取用户授权线路列表（供Feign调用）
     */
    @InnerAuth
    @GetMapping("/authorized-lines/{userId}")
    public R<List<Map<String, Object>>> getAuthorizedLines(
            @PathVariable("userId") Long userId,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source) {
        VpnUser vpnUser = userService.selectUserById(userId);
        if (vpnUser == null) {
            return R.fail("用户不存在");
        }

        Set<String> lineIdSet = lineAuthService.resolveAuthorizedLineIds(vpnUser);
        if (lineIdSet.isEmpty()) {
            return R.ok(Collections.emptyList());
        }

        return R.ok(lineAuthService.toAuthorizedLineVos(lineIdSet));
    }

    @RequiresPermissions("yianlian:user:queryAuth")
    @GetMapping("/{userId}/line-auths")
    public AjaxResult lineAuths(@PathVariable("userId") Long userId)
    {
        return success(lineAuthService.buildLineAuthView(userId));
    }

    @RequiresPermissions("vpn:user:list")
    @GetMapping("/list")
    public TableDataInfo list(VpnUser user) {
        startPage();
        List<VpnUser> list = userService.selectUserList(user);
        return getDataTable(list);
    }

    @Log(title = "用户管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("vpn:user:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, VpnUser user) {
        List<VpnUser> list = userService.selectUserList(user);
        ExcelUtil<VpnUser> util = new ExcelUtil<VpnUser>(VpnUser.class);
        util.exportExcel(response, list, "用户数据");
    }

    @RequiresPermissions("yianlian:user:syncLocal")
    @GetMapping("/sync-context/{appId}")
    public AjaxResult syncContext(@PathVariable String appId)
    {
        return success(localUserSyncService.buildLineSyncContext(appId));
    }

    @RequiresPermissions("yianlian:user:syncLocal")
    @Log(title = "线路用户管理", businessType = BusinessType.OTHER)
    @PostMapping("/sync-local")
    public AjaxResult syncLocal(@Validated @RequestBody VpnLineBatchSyncRequest request)
    {
        return success(localUserSyncService.syncUsersToLine(request));
    }

    @GetMapping("/deptTree")
    public AjaxResult deptTree(VpnDept dept) {
        List<VpnDept> depts = deptService.selectDeptList(dept);
        return success(deptService.buildDeptTreeSelect(depts));
    }

    @RequiresPermissions("vpn:user:query")
    @GetMapping(value = {"/", "/{userId}"})
    public AjaxResult getInfo(@PathVariable(value = "userId", required = false) Long userId,
                              @RequestParam(value = "appId", required = false) String appId) {
        AjaxResult ajax = AjaxResult.success();
        String roleAppId = appId;
        if (userId != null) {
            VpnUser vpnUser = userService.selectUserById(userId);
            ajax.put(AjaxResult.DATA_TAG, vpnUser);
            if (vpnUser != null && StringUtils.isEmpty(roleAppId)) {
                roleAppId = vpnUser.getAppId();
            }
            List<Long> roleIds = roleService.selectRoleListByUserId(userId);
            ajax.put("roleIds", roleIds != null ? roleIds : Collections.emptyList());
        }
        VpnRole roleQuery = new VpnRole();
        roleQuery.setAppId(roleAppId);
        ajax.put("roles", roleService.selectRoleList(roleQuery));
        return ajax;
    }

    @RequiresPermissions("vpn:user:add")
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody VpnUser user) {
        return error("请先在本地用户管理中创建用户并同步到线路");
    }

    /**
     * 根据用户编号获取授权角色
     */
    @RequiresPermissions("vpn:user:query")
    @GetMapping("/authRole/{userId}")
    public AjaxResult authRole(@PathVariable("userId") Long userId) {
        AjaxResult ajax = AjaxResult.success();
        VpnUser user = userService.selectUserById(userId);
        ajax.put("user", user);
        VpnRole roleQuery = new VpnRole();
        if (user != null && StringUtils.isNotEmpty(user.getAppId())) {
            roleQuery.setAppId(user.getAppId());
        }
        List<VpnRole> roles = roleService.selectRoleList(roleQuery);
        List<Long> userRoleIds = roleService.selectRoleListByUserId(userId);
        Set<Long> userRoleIdSet = userRoleIds != null
            ? new HashSet<>(userRoleIds)
            : Collections.emptySet();
        for (VpnRole role : roles) {
            role.setFlag(userRoleIdSet.contains(role.getRoleId()));
        }
        ajax.put("roles", roles);
        return ajax;
    }

    /**
     * 用户授权角色
     */
    @RequiresPermissions("vpn:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.GRANT)
    @PutMapping("/authRole")
    public AjaxResult insertAuthRole(Long userId, Long[] roleIds) {
        VpnUser user = userService.selectUserById(userId);
        String appId = user != null ? user.getAppId() : null;
        syncOrchestrator.execute(SyncCommand.ofApi(SyncConstants.BIZ_VPN_USER, SyncConstants.OP_ASSIGN_ROLES,
            appId, userId, new AssignRolesPayload(userId, roleIds)));
        return success();
    }

    @RequiresPermissions("vpn:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody VpnUser user) {
        userService.checkUserAllowed(user);
        if (!userService.checkUserNameUnique(user)) {
            return error("修改用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        user.setUpdateBy(SecurityUtils.getUsername());
        syncOrchestrator.execute(SyncCommand.ofApi(SyncConstants.BIZ_VPN_USER, SyncConstants.OP_UPDATE,
            user.getAppId(), user.getUserId(), user));
        return success();
    }

    @RequiresPermissions("vpn:user:remove")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable Long[] userIds) {
        if (ArrayUtils.contains(userIds, SecurityUtils.getUserId())) {
            return error("当前用户不能删除");
        }
        boolean anyDeferred = false;
        for (Long userId : userIds) {
            VpnUser user = userService.selectUserById(userId);
            if (user == null) {
                continue;
            }
            try {
                syncOrchestrator.execute(SyncCommand.ofApi(SyncConstants.BIZ_VPN_USER, SyncConstants.OP_DELETE,
                    user.getAppId(), userId, null));
            } catch (SyncDeferredException e) {
                anyDeferred = true;
            }
        }
        if (anyDeferred) {
            throw new SyncDeferredException(null, "部分用户代理暂不可用，已加入补偿队列，稍后自动重试");
        }
        return success();
    }

    @RequiresPermissions("vpn:user:resetPwd")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody VpnUser user) {
        VpnUser dbUser = userService.selectUserById(user.getUserId());
        if (dbUser == null)
        {
            return error("用户不存在");
        }
        userService.checkUserAllowed(dbUser);
        String plainPassword = user.getPassword();
        user.setAppId(dbUser.getAppId());
        user.setUserName(dbUser.getUserName());
        user.setPassword(SecurityUtils.encryptPassword(plainPassword));
        user.setEncryptedPwd(aesUtils.encrypt(plainPassword));
        user.setUpdateBy(SecurityUtils.getUsername());
        syncOrchestrator.execute(SyncCommand.ofApi(SyncConstants.BIZ_VPN_USER, SyncConstants.OP_RESET_PASSWORD,
            dbUser.getAppId(), user.getUserId(), new ResetPwdPayload(user, plainPassword)));
        return success();
    }

    @RequiresPermissions("vpn:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody VpnUser user) {
        VpnUser dbUser = userService.selectUserById(user.getUserId());
        if (dbUser == null)
        {
            return error("用户不存在");
        }
        userService.checkUserAllowed(dbUser);
        user.setAppId(dbUser.getAppId());
        user.setUserName(dbUser.getUserName());
        user.setUpdateBy(SecurityUtils.getUsername());
        syncOrchestrator.execute(SyncCommand.ofApi(SyncConstants.BIZ_VPN_USER, SyncConstants.OP_CHANGE_STATUS,
            dbUser.getAppId(), user.getUserId(), user));
        return success();
    }

    @InnerAuth
    @GetMapping("/info/{username}")
    public R<VpnLoginUser> getUserInfo(@PathVariable("username") String username,
                                       @RequestParam(value = "appId", required = false) String appId,
                                       @RequestHeader(SecurityConstants.FROM_SOURCE) String source) {
        VpnUser vpnUser;
        if (StringUtils.isNotEmpty(appId)) {
            vpnUser = userService.selectUserByUserNameAndAppId(username, appId);
        } else {
            vpnUser = userService.selectUserByUserName(username);
        }
        if (vpnUser == null) {
            return R.fail("用户不存在");
        }
        VpnUserInfo apiUser = new VpnUserInfo();
        apiUser.setUserId(vpnUser.getUserId());
        apiUser.setUserName(vpnUser.getUserName());
        apiUser.setNickName(vpnUser.getNickName());
        apiUser.setEmail(vpnUser.getEmail());
        apiUser.setPhonenumber(vpnUser.getPhonenumber());
        apiUser.setSex(vpnUser.getSex());
        apiUser.setAvatar(vpnUser.getAvatar());
        apiUser.setPassword(vpnUser.getPassword());
        apiUser.setStatus(vpnUser.getStatus());
        apiUser.setDelFlag(vpnUser.getDelFlag());
        apiUser.setLoginIp(vpnUser.getLoginIp());
        apiUser.setLoginDate(vpnUser.getLoginDate());
        apiUser.setDeptId(vpnUser.getDeptId());
        apiUser.setCreateBy(vpnUser.getCreateBy());
        apiUser.setCreateTime(vpnUser.getCreateTime());
        apiUser.setUpdateBy(vpnUser.getUpdateBy());
        apiUser.setUpdateTime(vpnUser.getUpdateTime());
        apiUser.setRemark(vpnUser.getRemark());

        VpnLoginUser vpnLoginUser = new VpnLoginUser();
        vpnLoginUser.setVpnUser(apiUser);
        vpnLoginUser.setUserid(vpnUser.getUserId());
        vpnLoginUser.setUsername(vpnUser.getUserName());
        List<VpnRole> roleList = roleService.selectUserRolesByUserId(vpnUser.getUserId());
        if (roleList != null && !roleList.isEmpty())
        {
            String effectiveAppId = StringUtils.isNotEmpty(appId) ? appId : vpnUser.getAppId();
            Set<String> roleKeys = roleList.stream()
                .filter(role -> includeRoleKeyForLogin(role, effectiveAppId))
                .map(SyncProxyConstants::resolveRoleKey)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
            vpnLoginUser.setRoles(roleKeys);
        }
        return R.ok(vpnLoginUser);
    }

    private boolean includeRoleKeyForLogin(VpnRole role, String lineAppId)
    {
        if (SyncProxyConstants.ROLE_SYNC_PROXY.equals(SyncProxyConstants.resolveRoleKey(role)))
        {
            return SyncProxyConstants.matchesSyncProxyRole(role, lineAppId);
        }
        return true;
    }

    @InnerAuth
    @PutMapping("/recordlogin")
    public R<Boolean> recordUserLogin(@RequestBody VpnUserInfo vpnUser, @RequestHeader(SecurityConstants.FROM_SOURCE) String source) {
        VpnUser user = new VpnUser();
        user.setUserId(vpnUser.getUserId());
        user.setLoginIp(vpnUser.getLoginIp());
        user.setLoginDate(vpnUser.getLoginDate());
        userService.updateUserLogin(user);
        return R.ok(true);
    }

    @InnerAuth
    @PutMapping("/changePassword")
    public R<Boolean> changePassword(@RequestBody VpnChangePasswordRequest request, @RequestHeader(SecurityConstants.FROM_SOURCE) String source) {
        String username = request.getUsername();
        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();
        String appId = request.getAppId();

        VpnUser vpnUser;
        if (StringUtils.isNotEmpty(appId)) {
            vpnUser = userService.selectUserByUserNameAndAppId(username, appId);
        } else {
            vpnUser = userService.selectUserByUserName(username);
        }
        if (vpnUser == null) {
            return R.fail("用户不存在");
        }
        if (UserStatus.DELETED.getCode().equals(vpnUser.getDelFlag())) {
            return R.fail("用户已被删除");
        }
        if (UserStatus.DISABLE.getCode().equals(vpnUser.getStatus())) {
            return R.fail("用户已停用");
        }
        if (!SecurityUtils.matchesPassword(oldPassword, vpnUser.getPassword())) {
            return R.fail("旧密码错误");
        }
        if (SecurityUtils.matchesPassword(newPassword, vpnUser.getPassword())) {
            return R.fail("新密码不能与旧密码相同");
        }

        userService.updatePasswordWithSync(vpnUser, oldPassword, newPassword);
        return R.ok(true);
    }

    private AjaxResult validateAppId(String appId) {
        if (StringUtils.isEmpty(appId)) {
            return error("线路不能为空");
        }
        return null;
    }
}
