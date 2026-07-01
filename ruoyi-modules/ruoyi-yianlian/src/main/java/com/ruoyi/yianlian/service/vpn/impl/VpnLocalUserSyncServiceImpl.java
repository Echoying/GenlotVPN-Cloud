package com.ruoyi.yianlian.service.vpn.impl;

import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.domain.VpnDeptYianlianMapping;
import com.ruoyi.yianlian.domain.VpnLocalUser;
import com.ruoyi.yianlian.domain.VpnUser;
import com.ruoyi.yianlian.domain.vo.VpnLocalUserSyncRequest;
import com.ruoyi.yianlian.mapper.VpnUserMapper;
import com.ruoyi.yianlian.service.IVpnDeptYianlianMappingService;
import com.ruoyi.yianlian.service.vpn.IVpnLocalUserService;
import com.ruoyi.yianlian.service.vpn.IVpnLocalUserSyncService;
import com.ruoyi.yianlian.service.vpn.IVpnUserService;
import com.ruoyi.yianlian.utils.AesUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 本地用户同步到线路
 */
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
    private AesUtils aesUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncToLine(VpnLocalUserSyncRequest request)
    {
        VpnLocalUser localUser = localUserService.selectLocalUserById(request.getLocalUserId());
        if (localUser == null)
        {
            throw new ServiceException("本地用户不存在");
        }
        if (!"0".equals(localUser.getStatus()))
        {
            throw new ServiceException("本地用户已停用，无法同步");
        }
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

        VpnUser existing = userMapper.selectUserByLocalUserIdAndAppId(request.getLocalUserId(), appId);
        if (existing != null)
        {
            updateExistingLineUser(existing, localUser, request);
            return;
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

    /**
     * 更新已同步的线路用户：刷新部门、角色及基础资料，并同步易安联
     */
    private void updateExistingLineUser(VpnUser existing, VpnLocalUser localUser, VpnLocalUserSyncRequest request)
    {
        existing.setDeptId(request.getDeptId());
        existing.setNickName(localUser.getNickName());
        existing.setEmail(localUser.getEmail());
        existing.setPhonenumber(localUser.getPhonenumber());
        existing.setSex(localUser.getSex());
        existing.setAvatar(localUser.getAvatar());
        existing.setStatus(localUser.getStatus());
        existing.setUpdateBy(SecurityUtils.getUsername());
        existing.setRoleIds(resolveRoleIds(request.getRoleIds()));

        int rows = userService.updateUserWithSync(existing);
        if (rows <= 0)
        {
            throw new ServiceException("更新线路用户同步信息失败");
        }
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
