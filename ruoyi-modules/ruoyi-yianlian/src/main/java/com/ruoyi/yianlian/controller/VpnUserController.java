package com.ruoyi.yianlian.controller;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.client.dto.YiAnLianDeptListRequest;
import com.ruoyi.yianlian.client.dto.YiAnLianDeptListResp;
import com.ruoyi.yianlian.client.dto.YiAnLianUserPasswordResetRequest;
import com.ruoyi.yianlian.client.dto.vo.YiAnLianDeptVO;
import com.ruoyi.yianlian.client.dto.YiAnLianUserCreateResultItem;
import com.ruoyi.yianlian.client.dto.vo.YiAnLianUserVO;
import com.ruoyi.yianlian.domain.LineApp;
import com.ruoyi.yianlian.domain.VpnDept;
import com.ruoyi.yianlian.domain.VpnUser;
import com.ruoyi.yianlian.service.vpn.IVpnDeptService;
import com.ruoyi.yianlian.service.vpn.IVpnLineAppService;
import com.ruoyi.yianlian.service.vpn.IVpnRoleService;
import com.ruoyi.yianlian.service.vpn.IVpnUserService;
import com.ruoyi.yianlian.service.yianlian.IYiAnLianDeptService;
import com.ruoyi.yianlian.service.yianlian.IYiAnLianUserService;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户信息
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/vpn/user")
public class VpnUserController extends BaseController {
    private static final Logger log = LoggerFactory.getLogger(VpnUserController.class);

    @Autowired
    private IVpnUserService userService;

    @Autowired
    private IVpnRoleService roleService;

    @Autowired
    private IYiAnLianUserService yiAnLianUserService;

    @Autowired
    private IYiAnLianDeptService yiAnLianDeptService;

    @Autowired
    private IVpnLineAppService lineAppService;

    @Autowired
    private IVpnDeptService deptService;

    /**
     * 获取用户列表
     */
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

    /**
     * 获取部门树列表
     */
    @GetMapping("/deptTree")
    public AjaxResult deptTree(VpnDept dept) {
        List<VpnDept> depts = deptService.selectDeptList(dept);
        return success(deptService.buildDeptTreeSelect(depts));
    }

    /**
     * 根据用户编号获取详细信息
     */
    @RequiresPermissions("vpn:user:query")
    @GetMapping(value = {"/", "/{userId}"})
    public AjaxResult getInfo(@PathVariable(value = "userId", required = false) Long userId) {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("roles", roleService.selectRoleAll());
        if (userId != null) {
            VpnUser vpnUser = userService.selectUserById(userId);
            ajax.put(AjaxResult.DATA_TAG, vpnUser);
            ajax.put("roleIds", vpnUser.getRoles().stream().map(r -> r.getRoleId()).collect(Collectors.toList()));
        }
        return ajax;
    }

    /**
     * 新增用户
     */
    @RequiresPermissions("vpn:user:add")
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody VpnUser user) {
        if (!userService.checkUserNameUnique(user)) {
            return error("新增用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        // 保存明文密码，用于同步到易安联
        String plainPassword = user.getPassword();
        user.setCreateBy(SecurityUtils.getUsername());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        int row = userService.insertUser(user);

        // 同步到易安联
        try {
            List<LineApp> lineApps = lineAppService.selectLineAppList(new LineApp());
            for (LineApp lineApp : lineApps) {
                // 查找用户部门在易安联中的ID
                String yiAnLianDeptId = findYiAnLianDeptId(lineApp.getAppId(), user.getDeptId());

                // 构建易安联用户
                YiAnLianUserVO yiAnLianUser = buildYiAnLianUserVO(user, yiAnLianDeptId);
                yiAnLianUser.setPassword(plainPassword);

                List<YiAnLianUserCreateResultItem> results = yiAnLianUserService.create(lineApp.getAppId(), Collections.singletonList(yiAnLianUser));
                // 保存易安联返回的用户ID
                if (results != null && !results.isEmpty()) {
                    YiAnLianUserCreateResultItem resultItem = results.get(0);
                    if (resultItem.getData() != null && resultItem.getData().getId() != null) {
                        user.setYianlianId(resultItem.getData().getId());
                        userService.updateUser(user);
                    }
                }
            }
        } catch (Exception e) {
            log.error("同步用户到易安联失败", e);
        }

        return toAjax(row);
    }

    /**
     * 修改用户
     */
    @RequiresPermissions("vpn:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody VpnUser user) {
        userService.checkUserAllowed(user);
        if (!userService.checkUserNameUnique(user)) {
            return error("修改用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        user.setUpdateBy(SecurityUtils.getUsername());
        int row = userService.updateUser(user);

        // 同步到易安联
        try {
            List<LineApp> lineApps = lineAppService.selectLineAppList(new LineApp());
            for (LineApp lineApp : lineApps) {
                if (user.getYianlianId() != null) {
                    // 查找用户部门在易安联中的ID
                    String yiAnLianDeptId = findYiAnLianDeptId(lineApp.getAppId(), user.getDeptId());

                    // 使用本地存储的易安联ID构建远程用户
                    YiAnLianUserVO remoteUser = new YiAnLianUserVO();
                    remoteUser.setId(user.getYianlianId());
                    remoteUser.setUsername(user.getUserName());
                    remoteUser.setName(user.getNickName());
                    remoteUser.setMobile(user.getPhonenumber());
                    remoteUser.setEmail(user.getEmail());
                    remoteUser.setGender(user.getSex());
                    remoteUser.setStatus("0".equals(user.getStatus()) ? "enable" : "disable");
                    if (yiAnLianDeptId != null) {
                        remoteUser.setGroups(Collections.singletonList(yiAnLianDeptId));
                    }

                    yiAnLianUserService.update(lineApp.getAppId(), remoteUser);
                }
            }
        } catch (Exception e) {
            log.error("同步用户到易安联失败", e);
        }

        return toAjax(row);
    }

    /**
     * 删除用户
     */
    @RequiresPermissions("vpn:user:remove")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable Long[] userIds) {
        if (ArrayUtils.contains(userIds, SecurityUtils.getUserId())) {
            return error("当前用户不能删除");
        }

        // 删除前获取用户的易安联ID列表
        List<String> yiAnLianIds = new ArrayList<>();
        for (Long userId : userIds) {
            VpnUser vpnUser = userService.selectUserById(userId);
            if (vpnUser != null && vpnUser.getYianlianId() != null) {
                yiAnLianIds.add(vpnUser.getYianlianId());
            }
        }

        int row = userService.deleteUserByIds(userIds);

        // 同步到易安联
        try {
            if (!yiAnLianIds.isEmpty()) {
                List<LineApp> lineApps = lineAppService.selectLineAppList(new LineApp());
                for (LineApp lineApp : lineApps) {
                    yiAnLianUserService.delete(lineApp.getAppId(), yiAnLianIds);
                }
            }
        } catch (Exception e) {
            log.error("同步用户到易安联失败", e);
        }

        return toAjax(row);
    }

    /**
     * 重置密码
     */
    @RequiresPermissions("vpn:user:resetPwd")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody VpnUser user) {
        userService.checkUserAllowed(user);
        // 保存明文密码，用于同步到易安联
        String plainPassword = user.getPassword();
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        user.setUpdateBy(SecurityUtils.getUsername());
        int row = userService.resetPwd(user);

        // 同步到易安联
        try {
            VpnUser vpnUser = userService.selectUserById(user.getUserId());
            if (vpnUser != null && vpnUser.getYianlianId() != null) {
                List<LineApp> lineApps = lineAppService.selectLineAppList(new LineApp());
                for (LineApp lineApp : lineApps) {
                    YiAnLianUserPasswordResetRequest resetRequest = new YiAnLianUserPasswordResetRequest();
                    resetRequest.setAppId(lineApp.getAppId());
                    resetRequest.setUsername(vpnUser.getUserName());
                    resetRequest.setNewPassword(plainPassword);
                    yiAnLianUserService.resetPassword(lineApp.getAppId(), resetRequest);
                }
            }
        } catch (Exception e) {
            log.error("同步密码重置到易安联失败", e);
        }

        return toAjax(row);
    }

    /**
     * 状态修改
     */
    @RequiresPermissions("vpn:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody VpnUser user) {
        userService.checkUserAllowed(user);
        user.setUpdateBy(SecurityUtils.getUsername());
        int row = userService.updateUserStatus(user);

        // 同步到易安联
        try {
            VpnUser vpnUser = userService.selectUserById(user.getUserId());
            if (vpnUser != null && vpnUser.getYianlianId() != null) {
                List<LineApp> lineApps = lineAppService.selectLineAppList(new LineApp());
                for (LineApp lineApp : lineApps) {
                    YiAnLianUserVO remoteUser = new YiAnLianUserVO();
                    remoteUser.setId(vpnUser.getYianlianId());
                    remoteUser.setUsername(vpnUser.getUserName());
                    remoteUser.setName(vpnUser.getNickName());
                    remoteUser.setStatus("0".equals(user.getStatus()) ? "enable" : "disable");
                    yiAnLianUserService.update(lineApp.getAppId(), remoteUser);
                }
            }
        } catch (Exception e) {
            log.error("同步用户状态到易安联失败", e);
        }

        return toAjax(row);
    }


    /**
     * 查找本地部门在易安联中对应的部门ID
     *
     * @param appId  应用ID
     * @param deptId 本地部门ID
     * @return 易安联部门ID，未找到返回null
     */
    private String findYiAnLianDeptId(String appId, Long deptId) {
        if (deptId == null) {
            return null;
        }
        VpnDept dept = deptService.selectDeptById(deptId);
        if (dept == null) {
            return null;
        }
        YiAnLianDeptListRequest deptListRequest = new YiAnLianDeptListRequest();
        deptListRequest.setAppId(appId);
        YiAnLianDeptListResp deptListResp = yiAnLianDeptService.getDeptList(deptListRequest);
        List<YiAnLianDeptVO> yiAnLianDepts = deptListResp != null ? deptListResp.getData() : null;
        if (yiAnLianDepts != null) {
            YiAnLianDeptVO matched = yiAnLianDepts.stream()
                    .filter(d -> dept.getDeptName().equals(d.getName()))
                    .findFirst()
                    .orElse(null);
            if (matched != null) {
                return matched.getId();
            }
        }
        return null;
    }

    /**
     * 构建易安联用户VO
     *
     * @param user           本地用户
     * @param yiAnLianDeptId 易安联部门ID
     * @return 易安联用户VO
     */
    private YiAnLianUserVO buildYiAnLianUserVO(VpnUser user, String yiAnLianDeptId) {
        YiAnLianUserVO vo = new YiAnLianUserVO();
        vo.setUsername(user.getUserName());
        vo.setName(user.getNickName());
        vo.setMobile(user.getPhonenumber());
        vo.setEmail(user.getEmail());
        vo.setGender(user.getSex());
        vo.setStatus("0".equals(user.getStatus()) ? "enable" : "disable");
        if (yiAnLianDeptId != null) {
            vo.setGroups(Collections.singletonList(yiAnLianDeptId));
        }
        return vo;
    }

}
