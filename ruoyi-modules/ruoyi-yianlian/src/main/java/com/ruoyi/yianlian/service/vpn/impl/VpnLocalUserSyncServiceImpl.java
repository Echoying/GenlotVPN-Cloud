package com.ruoyi.yianlian.service.vpn.impl;

import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.domain.LineApp;
import com.ruoyi.yianlian.domain.VpnDeptYianlianMapping;
import com.ruoyi.yianlian.domain.VpnLocalUser;
import com.ruoyi.yianlian.domain.VpnUser;
import com.ruoyi.yianlian.domain.vo.VpnLocalUserBatchSyncRequest;
import com.ruoyi.yianlian.domain.vo.VpnLocalUserSyncLineItem;
import com.ruoyi.yianlian.domain.vo.VpnLocalUserSyncLineResult;
import com.ruoyi.yianlian.domain.vo.VpnLocalUserSyncRequest;
import com.ruoyi.yianlian.mapper.VpnUserMapper;
import com.ruoyi.yianlian.service.IVpnDeptYianlianMappingService;
import com.ruoyi.yianlian.service.vpn.IVpnLineAppService;
import com.ruoyi.yianlian.service.vpn.IVpnLocalUserService;
import com.ruoyi.yianlian.service.vpn.IVpnLocalUserSyncService;
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
import java.util.List;
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
    private IVpnLineAppService lineAppService;

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
