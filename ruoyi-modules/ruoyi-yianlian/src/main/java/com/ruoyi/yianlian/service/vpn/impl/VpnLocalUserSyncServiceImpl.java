package com.ruoyi.yianlian.service.vpn.impl;

import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.domain.LineApp;
import com.ruoyi.yianlian.domain.VpnDeptYianlianMapping;
import com.ruoyi.yianlian.domain.VpnLocalUser;
import com.ruoyi.yianlian.domain.VpnRole;
import com.ruoyi.yianlian.domain.VpnRoleYianlianMapping;
import com.ruoyi.yianlian.domain.VpnUser;
import com.ruoyi.yianlian.domain.vo.VpnLocalUserBatchSyncRequest;
import com.ruoyi.yianlian.domain.vo.VpnLocalUserSyncLineItem;
import com.ruoyi.yianlian.domain.vo.VpnLocalUserSyncLineResult;
import com.ruoyi.yianlian.domain.vo.VpnLocalUserSyncRequest;
import com.ruoyi.yianlian.domain.vo.VpnLineBatchSyncRequest;
import com.ruoyi.yianlian.domain.vo.VpnLineBatchSyncUserResult;
import com.ruoyi.yianlian.domain.vo.VpnLineSyncContextVO;
import com.ruoyi.yianlian.domain.vo.VpnLineSyncGroupItem;
import com.ruoyi.yianlian.domain.vo.VpnLineSyncedGroupVO;
import com.ruoyi.yianlian.domain.vo.VpnLineSyncedUserVO;
import com.ruoyi.yianlian.mapper.VpnUserMapper;
import com.ruoyi.yianlian.service.IVpnDeptYianlianMappingService;
import com.ruoyi.yianlian.service.IVpnRoleYianlianMappingService;
import com.ruoyi.yianlian.domain.VpnDept;
import com.ruoyi.yianlian.service.vpn.IVpnDeptService;
import com.ruoyi.yianlian.service.vpn.IVpnLineAppService;
import com.ruoyi.yianlian.service.vpn.IVpnLocalUserService;
import com.ruoyi.yianlian.service.vpn.IVpnLocalUserSyncService;
import com.ruoyi.yianlian.service.vpn.IVpnRoleService;
import com.ruoyi.yianlian.service.vpn.IVpnUserService;
import com.ruoyi.yianlian.utils.AesUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class VpnLocalUserSyncServiceImpl implements IVpnLocalUserSyncService
{
    @Autowired
    private IVpnLocalUserService localUserService;

    @Autowired
    private IVpnUserService userService;

    @Autowired
    private VpnUserMapper userMapper;

    @Autowired
    private IVpnDeptYianlianMappingService deptMappingService;

    @Autowired
    private IVpnRoleYianlianMappingService roleMappingService;

    @Autowired
    private IVpnRoleService roleService;

    @Autowired
    private IVpnLineAppService lineAppService;

    @Autowired
    private IVpnDeptService deptService;

    @Autowired
    private AesUtils aesUtils;

    @Lazy
    @Autowired
    private VpnLocalUserSyncServiceImpl self;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncToLine(VpnLocalUserSyncRequest request)
    {
        VpnLocalUser localUser = loadAndValidateLocalUser(request.getLocalUserId());
        doSyncToLine(localUser, request);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void syncToLineInNewTx(VpnLocalUserSyncRequest request)
    {
        VpnLocalUser localUser = loadAndValidateLocalUser(request.getLocalUserId());
        doSyncToLine(localUser, request);
    }

    @Override
    public List<VpnLocalUserSyncLineResult> syncToLines(VpnLocalUserBatchSyncRequest request)
    {
        validateUniqueAppIds(request.getLines());
        loadAndValidateLocalUser(request.getLocalUserId());

        List<VpnLocalUserSyncLineResult> results = new ArrayList<>();
        for (VpnLocalUserSyncLineItem item : request.getLines())
        {
            VpnLocalUserSyncLineResult result = new VpnLocalUserSyncLineResult();
            result.setAppId(item.getAppId());
            result.setAppName(resolveLineName(item.getAppId()));
            try
            {
                self.syncToLineInNewTx(toSingleRequest(request.getLocalUserId(), item));
                result.setSuccess(true);
                result.setUpdated(false);
                result.setMessage("已同步到线路");
            }
            catch (Exception e)
            {
                result.setSuccess(false);
                result.setUpdated(false);
                result.setMessage(resolveErrorMessage(e));
            }
            results.add(result);
        }
        return results;
    }

    @Override
    public VpnLineSyncContextVO buildLineSyncContext(String appId)
    {
        validateLineApp(appId);
        VpnLineSyncContextVO context = new VpnLineSyncContextVO();
        context.setUnsyncedUsers(localUserService.selectUnsyncedByAppId(appId));
        context.setSyncedGroups(buildSyncedGroups(userMapper.selectSyncedLocalUsersByAppId(appId)));
        return context;
    }

    @Override
    public List<VpnLineBatchSyncUserResult> syncUsersToLine(VpnLineBatchSyncRequest request)
    {
        String appId = request.getAppId();
        validateLineApp(appId);
        validateLineSyncGroups(request.getAppId(), request.getGroups());

        List<VpnLineBatchSyncUserResult> results = new ArrayList<>();
        for (VpnLineSyncGroupItem group : request.getGroups())
        {
            if (group.getLocalUserIds() == null || group.getLocalUserIds().isEmpty())
            {
                continue;
            }
            String deptName = resolveDeptName(group.getDeptId());
            for (Long localUserId : group.getLocalUserIds())
            {
                VpnLineBatchSyncUserResult result = new VpnLineBatchSyncUserResult();
                result.setLocalUserId(localUserId);
                result.setDeptId(group.getDeptId());
                result.setDeptName(deptName);
                VpnLocalUser localUser = localUserService.selectLocalUserById(localUserId);
                if (localUser != null)
                {
                    result.setUserName(localUser.getUserName());
                    result.setNickName(localUser.getNickName());
                }
                try
                {
                    VpnLocalUserSyncRequest single = new VpnLocalUserSyncRequest();
                    single.setLocalUserId(localUserId);
                    single.setAppId(appId);
                    single.setDeptId(group.getDeptId());
                    single.setRoleIds(Collections.emptyList());
                    self.syncToLineInNewTx(single);
                    result.setSuccess(true);
                    result.setMessage("已同步到线路");
                }
                catch (Exception e)
                {
                    result.setSuccess(false);
                    result.setMessage(resolveErrorMessage(e));
                }
                results.add(result);
            }
        }
        if (results.isEmpty())
        {
            throw new ServiceException("请至少选择一名待同步用户");
        }
        return results;
    }

    private void validateLineApp(String appId)
    {
        if (StringUtils.isEmpty(appId))
        {
            throw new ServiceException("线路不能为空");
        }
        LineApp lineApp = lineAppService.selectLineAppById(appId);
        if (lineApp == null)
        {
            throw new ServiceException("线路不存在");
        }
        if (!"0".equals(lineApp.getStatus()))
        {
            throw new ServiceException("线路已停用，无法同步");
        }
    }

    private void validateLineSyncGroups(String appId, List<VpnLineSyncGroupItem> groups)
    {
        if (groups == null || groups.isEmpty())
        {
            throw new ServiceException("请至少配置一个部门分组");
        }
        Set<Long> deptIds = new HashSet<>();
        Set<Long> localUserIds = new HashSet<>();
        boolean hasPendingUser = false;
        for (VpnLineSyncGroupItem group : groups)
        {
            if (group.getDeptId() == null)
            {
                throw new ServiceException("请为每个分组选择部门");
            }
            if (!deptIds.add(group.getDeptId()))
            {
                throw new ServiceException("部门不能重复选择");
            }
            VpnDeptYianlianMapping deptMapping = deptMappingService.selectByDeptIdAndAppId(group.getDeptId(), appId);
            if (deptMapping == null || StringUtils.isEmpty(deptMapping.getYianlianId()))
            {
                throw new ServiceException("请先将部门同步到该线路");
            }
            if (group.getLocalUserIds() == null || group.getLocalUserIds().isEmpty())
            {
                continue;
            }
            for (Long localUserId : group.getLocalUserIds())
            {
                if (localUserId == null)
                {
                    continue;
                }
                if (!localUserIds.add(localUserId))
                {
                    throw new ServiceException("待同步用户不能重复选择");
                }
                hasPendingUser = true;
            }
        }
        if (!hasPendingUser)
        {
            throw new ServiceException("请至少选择一名待同步用户");
        }
    }

    private List<VpnLineSyncedGroupVO> buildSyncedGroups(List<VpnLineSyncedUserVO> users)
    {
        if (users == null || users.isEmpty())
        {
            return Collections.emptyList();
        }
        Map<Long, VpnLineSyncedGroupVO> groupMap = new LinkedHashMap<>();
        for (VpnLineSyncedUserVO user : users)
        {
            Long deptId = user.getDeptId();
            if (deptId == null)
            {
                continue;
            }
            VpnLineSyncedGroupVO group = groupMap.computeIfAbsent(deptId, id -> {
                VpnLineSyncedGroupVO g = new VpnLineSyncedGroupVO();
                g.setDeptId(id);
                g.setDeptName(user.getDeptName());
                g.setUsers(new ArrayList<>());
                return g;
            });
            group.getUsers().add(user);
        }
        return new ArrayList<>(groupMap.values());
    }

    private String resolveDeptName(Long deptId)
    {
        if (deptId == null)
        {
            return "";
        }
        VpnDept dept = deptService.selectDeptById(deptId);
        return dept != null ? dept.getDeptName() : String.valueOf(deptId);
    }

    private void validateUniqueAppIds(List<VpnLocalUserSyncLineItem> lines)
    {
        Set<String> appIds = new HashSet<>();
        for (VpnLocalUserSyncLineItem item : lines)
        {
            if (StringUtils.isEmpty(item.getAppId()))
            {
                continue;
            }
            if (!appIds.add(item.getAppId()))
            {
                throw new ServiceException("线路不能重复选择");
            }
        }
    }

    private VpnLocalUserSyncRequest toSingleRequest(Long localUserId, VpnLocalUserSyncLineItem item)
    {
        VpnLocalUserSyncRequest request = new VpnLocalUserSyncRequest();
        request.setLocalUserId(localUserId);
        request.setAppId(item.getAppId());
        request.setDeptId(item.getDeptId());
        request.setRoleIds(item.getRoleIds());
        return request;
    }

    private VpnLocalUser loadAndValidateLocalUser(Long localUserId)
    {
        VpnLocalUser localUser = localUserService.selectLocalUserById(localUserId);
        if (localUser == null)
        {
            throw new ServiceException("本地用户不存在");
        }
        if (!"0".equals(localUser.getStatus()))
        {
            throw new ServiceException("本地用户已停用，无法同步");
        }
        return localUser;
    }

    private void doSyncToLine(VpnLocalUser localUser, VpnLocalUserSyncRequest request)
    {
        String appId = request.getAppId();
        if (StringUtils.isEmpty(appId))
        {
            throw new ServiceException("线路不能为空");
        }
        VpnDeptYianlianMapping deptMapping = deptMappingService.selectByDeptIdAndAppId(request.getDeptId(), appId);
        if (deptMapping == null || StringUtils.isEmpty(deptMapping.getYianlianId()))
        {
            throw new ServiceException("请先将部门同步到该线路");
        }
        validateRoleMappings(appId, request.getRoleIds());

        VpnUser existing = userMapper.selectUserByLocalUserIdAndAppId(request.getLocalUserId(), appId);
        if (existing != null)
        {
            throw new ServiceException("该用户已同步到此线路，请在线路用户管理中修改");
        }

        String plainPassword = decryptPassword(localUser.getEncryptedPwd());
        if (StringUtils.isEmpty(plainPassword))
        {
            throw new ServiceException("无法解析本地用户密码，请重置密码后重试");
        }

        VpnUser lineUser = buildLineUserFromLocal(localUser, appId, request.getDeptId());
        lineUser.setCreateBy(SecurityUtils.getUsername());
        lineUser.setRoleIds(resolveRoleIds(request.getRoleIds()));

        if (!userService.checkUserNameUnique(lineUser))
        {
            throw new ServiceException("该线路下登录账号已存在");
        }

        int rows = userService.insertUserWithSync(lineUser, plainPassword);
        if (rows <= 0)
        {
            throw new ServiceException("同步线路用户失败");
        }
    }

    private void validateRoleMappings(String appId, List<Long> roleIds)
    {
        if (roleIds == null || roleIds.isEmpty())
        {
            return;
        }
        for (Long roleId : roleIds)
        {
            VpnRoleYianlianMapping mapping = roleMappingService.selectByRoleIdAndAppId(roleId, appId);
            if (mapping == null || StringUtils.isEmpty(mapping.getYianlianId()))
            {
                VpnRole role = roleService.selectRoleById(roleId);
                String roleName = role != null ? role.getRoleName() : String.valueOf(roleId);
                throw new ServiceException("请先将角色「" + roleName + "」同步到该线路");
            }
        }
    }

    private String resolveLineName(String appId)
    {
        if (StringUtils.isEmpty(appId))
        {
            return appId;
        }
        LineApp lineApp = lineAppService.selectLineAppById(appId);
        return lineApp != null && StringUtils.isNotEmpty(lineApp.getAppName())
            ? lineApp.getAppName()
            : appId;
    }

    private String resolveErrorMessage(Exception e)
    {
        if (e instanceof ServiceException)
        {
            return e.getMessage();
        }
        return StringUtils.isNotEmpty(e.getMessage()) ? e.getMessage() : "同步失败";
    }

    private VpnUser buildLineUserFromLocal(VpnLocalUser localUser, String appId, Long deptId)
    {
        VpnUser lineUser = new VpnUser();
        lineUser.setLocalUserId(localUser.getLocalUserId());
        lineUser.setAppId(appId);
        lineUser.setDeptId(deptId);
        lineUser.setUserName(localUser.getUserName());
        lineUser.setNickName(localUser.getNickName());
        lineUser.setEmail(localUser.getEmail());
        lineUser.setPhonenumber(localUser.getPhonenumber());
        lineUser.setSex(localUser.getSex());
        lineUser.setAvatar(localUser.getAvatar());
        lineUser.setStatus(localUser.getStatus());
        lineUser.setPassword(localUser.getPassword());
        lineUser.setEncryptedPwd(localUser.getEncryptedPwd());
        return lineUser;
    }

    private List<Long> resolveRoleIds(List<Long> roleIds)
    {
        return roleIds != null ? roleIds : Collections.emptyList();
    }

    private String decryptPassword(String encryptedPwd)
    {
        if (StringUtils.isEmpty(encryptedPwd))
        {
            return null;
        }
        try
        {
            return aesUtils.decrypt(encryptedPwd);
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
