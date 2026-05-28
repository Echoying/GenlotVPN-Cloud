package com.ruoyi.yianlian.controller;

import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.InnerAuth;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.api.domain.VpnUserInfo;
import com.ruoyi.yianlian.api.model.VpnLoginUser;
import com.ruoyi.yianlian.client.dto.YiAnLianUserPasswordResetRequest;
import com.ruoyi.yianlian.client.dto.YiAnLianUserCreateResultItem;
import com.ruoyi.yianlian.client.dto.vo.YiAnLianUserVO;
import com.ruoyi.yianlian.domain.LineApp;
import com.ruoyi.yianlian.domain.VpnDept;
import com.ruoyi.yianlian.domain.VpnDeptYianlianMapping;
import com.ruoyi.yianlian.domain.VpnUser;
import com.ruoyi.yianlian.domain.VpnUserYianlianMapping;
import com.ruoyi.yianlian.domain.YalDeptAuth;
import com.ruoyi.yianlian.domain.YalRoleAuth;
import com.ruoyi.yianlian.domain.YalUserAuth;
import com.ruoyi.yianlian.mapper.YalDeptAuthMapper;
import com.ruoyi.yianlian.mapper.YalRoleAuthMapper;
import com.ruoyi.yianlian.mapper.YalUserAuthMapper;
import com.ruoyi.yianlian.service.IVpnDeptYianlianMappingService;
import com.ruoyi.yianlian.service.vpn.IVpnDeptService;
import com.ruoyi.yianlian.service.vpn.IVpnLineAppService;
import com.ruoyi.yianlian.service.vpn.IVpnRoleService;
import com.ruoyi.yianlian.service.vpn.IVpnUserService;
import com.ruoyi.yianlian.service.vpn.IVpnUserYianlianMappingService;
import com.ruoyi.yianlian.service.yianlian.IYiAnLianUserService;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(VpnUserController.class);

    @Autowired
    private IVpnUserService userService;

    @Autowired
    private IVpnRoleService roleService;

    @Autowired
    private IYiAnLianUserService yiAnLianUserService;

    @Autowired
    private IVpnLineAppService lineAppService;

    @Autowired
    private IVpnDeptService deptService;

    @Autowired
    private IVpnUserYianlianMappingService userMappingService;

    @Autowired
    private IVpnDeptYianlianMappingService deptMappingService;

    @Autowired
    private YalDeptAuthMapper yalDeptAuthMapper;

    @Autowired
    private YalRoleAuthMapper yalRoleAuthMapper;

    @Autowired
    private YalUserAuthMapper yalUserAuthMapper;

    /**
     * 获取用户授权线路列表（供Feign调用）
     *
     * @param userId 用户ID
     * @param source 请求来源
     * @return 授权线路列表
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

        Set<String> lineIdSet = new LinkedHashSet<>();

        // 1. 部门授权
        if (vpnUser.getDeptId() != null) {
            List<YalDeptAuth> deptAuths = yalDeptAuthMapper.selectYalDeptAuthByDeptId(vpnUser.getDeptId());
            for (YalDeptAuth auth : deptAuths) {
                if (auth.getLineId() != null) {
                    lineIdSet.add(auth.getLineId());
                }
            }
        }

        // 2. 角色授权
        if (vpnUser.getRoles() != null) {
            for (com.ruoyi.yianlian.domain.VpnRole role : vpnUser.getRoles()) {
                List<YalRoleAuth> roleAuths = yalRoleAuthMapper.selectYalRoleAuthByRoleId(role.getRoleId());
                for (YalRoleAuth auth : roleAuths) {
                    if (auth.getLineId() != null) {
                        lineIdSet.add(auth.getLineId());
                    }
                }
            }
        }

        // 3. 用户授权
        List<YalUserAuth> userAuths = yalUserAuthMapper.selectYalUserAuthByUserId(userId);
        for (YalUserAuth auth : userAuths) {
            if (auth.getLineId() != null) {
                lineIdSet.add(auth.getLineId());
            }
        }

        if (lineIdSet.isEmpty()) {
            return R.ok(Collections.emptyList());
        }

        // 查询线路详情，过滤状态正常的线路
        List<LineApp> allLines = lineAppService.selectLineAppList(new LineApp());
        List<Map<String, Object>> result = allLines.stream()
            .filter(line -> lineIdSet.contains(line.getAppId()) && "0".equals(line.getStatus()))
            .map(line -> {
                Map<String, Object> vo = new LinkedHashMap<>();
                vo.put("appId", line.getAppId());
                vo.put("appName", line.getAppName());
                vo.put("host", line.getHost());
                vo.put("srvPort", line.getSrvPort());
                vo.put("spaPort", line.getSpaPort());
                return vo;
            })
            .collect(Collectors.toList());

        return R.ok(result);
    }

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
        if (row > 0) {
            List<LineApp> lineApps = lineAppService.selectLineAppList(new LineApp());
            for (LineApp lineApp : lineApps) {
                // 通过部门映射表获取yianlian部门ID
                String yiAnLianDeptId = null;
                if (user.getDeptId() != null) {
                    VpnDeptYianlianMapping deptMapping = deptMappingService.selectByDeptIdAndAppId(user.getDeptId(), lineApp.getAppId());
                    if (deptMapping != null) {
                        yiAnLianDeptId = deptMapping.getYianlianId();
                    }
                }

                // 构建易安联用户
                YiAnLianUserVO yiAnLianUser = buildYiAnLianUserVO(user, yiAnLianDeptId);
                yiAnLianUser.setPassword(plainPassword);

                List<YiAnLianUserCreateResultItem> results = yiAnLianUserService.create(lineApp.getAppId(), Collections.singletonList(yiAnLianUser));
                // 创建返回的数据里直接有ID，保存映射
                if (results != null && !results.isEmpty()) {
                    YiAnLianUserCreateResultItem resultItem = results.get(0);
                    if (resultItem.getData() != null && resultItem.getData().getId() != null) {
                        VpnUserYianlianMapping mapping = new VpnUserYianlianMapping();
                        mapping.setUserId(user.getUserId());
                        mapping.setAppId(lineApp.getAppId());
                        mapping.setYianlianId(resultItem.getData().getId());
                        userMappingService.insert(mapping);
                    }
                }
            }
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
        if (row > 0) {
            List<LineApp> lineApps = lineAppService.selectLineAppList(new LineApp());
            for (LineApp lineApp : lineApps) {
                // 查询映射表获取yianlian用户ID
                VpnUserYianlianMapping mapping = userMappingService.selectByUserIdAndAppId(user.getUserId(), lineApp.getAppId());
                if (mapping != null) {
                    // 通过部门映射表获取yianlian部门ID
                    String yiAnLianDeptId = null;
                    if (user.getDeptId() != null) {
                        VpnDeptYianlianMapping deptMapping = deptMappingService.selectByDeptIdAndAppId(user.getDeptId(), lineApp.getAppId());
                        if (deptMapping != null) {
                            yiAnLianDeptId = deptMapping.getYianlianId();
                        }
                    }

                    YiAnLianUserVO remoteUser = new YiAnLianUserVO();
                    remoteUser.setId(mapping.getYianlianId());
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

        int row = userService.deleteUserByIds(userIds);

        // 同步到易安联
        if (row > 0) {
            for (Long userId : userIds) {
                // 通过映射表获取yianlianId进行删除
                List<VpnUserYianlianMapping> mappings = userMappingService.selectByUserId(userId);
                for (VpnUserYianlianMapping mapping : mappings) {
                    boolean ret = yiAnLianUserService.delete(mapping.getAppId(), Collections.singletonList(mapping.getYianlianId()));
                    if (!ret) {
                        log.error("删除易安联用户失败, appId: {}, yianlianId: {}", mapping.getAppId(), mapping.getYianlianId());
                    }
                }
                // 删除映射记录
                userMappingService.deleteByUserId(userId);
            }
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
        if (row > 0) {
            VpnUser vpnUser = userService.selectUserById(user.getUserId());
            List<VpnUserYianlianMapping> mappings = userMappingService.selectByUserId(user.getUserId());
            for (VpnUserYianlianMapping mapping : mappings) {
                YiAnLianUserPasswordResetRequest resetRequest = new YiAnLianUserPasswordResetRequest();
                resetRequest.setAppId(mapping.getAppId());
                resetRequest.setUsername(vpnUser.getUserName());
                resetRequest.setNewPassword(plainPassword);
                yiAnLianUserService.resetPassword(mapping.getAppId(), resetRequest);
            }
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
        if (row > 0) {
            VpnUser vpnUser = userService.selectUserById(user.getUserId());
            List<VpnUserYianlianMapping> mappings = userMappingService.selectByUserId(user.getUserId());
            for (VpnUserYianlianMapping mapping : mappings) {
                YiAnLianUserVO remoteUser = new YiAnLianUserVO();
                remoteUser.setId(mapping.getYianlianId());
                remoteUser.setUsername(vpnUser.getUserName());
                remoteUser.setName(vpnUser.getNickName());
                remoteUser.setStatus("0".equals(user.getStatus()) ? "enable" : "disable");
                yiAnLianUserService.update(mapping.getAppId(), remoteUser);
            }
        }

        return toAjax(row);
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

    /**
     * 获取用户信息（供Feign调用）
     *
     * @param username 用户名
     * @param source   请求来源
     * @return 用户信息
     */
    @InnerAuth
    @GetMapping("/info/{username}")
    public R<VpnLoginUser> getUserInfo(@PathVariable("username") String username, @RequestHeader(SecurityConstants.FROM_SOURCE) String source) {
        VpnUser vpnUser = userService.selectUserByUserName(username);
        if (vpnUser == null) {
            return R.fail("用户不存在");
        }
        // 转换为API层的VpnUser
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

        // 构建VpnLoginUser
        VpnLoginUser vpnLoginUser = new VpnLoginUser();
        vpnLoginUser.setVpnUser(apiUser);
        vpnLoginUser.setUserid(vpnUser.getUserId());
        vpnLoginUser.setUsername(vpnUser.getUserName());

        // 查询角色权限（如果需要）
        // Set<String> roles = roleService.selectRolePermissionByUserId(vpnUser.getUserId());
        // vpnLoginUser.setRoles(roles);

        return R.ok(vpnLoginUser);
    }

    /**
     * 记录用户登录信息（供Feign调用）
     *
     * @param vpnUser VPN用户信息
     * @param source  请求来源
     * @return 结果
     */
    @InnerAuth
    @PutMapping("/recordlogin")
    public R<Boolean> recordUserLogin(@RequestBody VpnUserInfo vpnUser, @RequestHeader(SecurityConstants.FROM_SOURCE) String source) {
        VpnUser user = new VpnUser();
        user.setUserId(vpnUser.getUserId());
        user.setLoginIp(vpnUser.getLoginIp());
        user.setLoginDate(vpnUser.getLoginDate());
        userService.updateUser(user);
        return R.ok(true);
    }

}
