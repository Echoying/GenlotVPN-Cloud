package com.ruoyi.yianlian.service.vpn.impl;

import com.ruoyi.common.core.constant.UserConstants;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.domain.LineApp;
import com.ruoyi.yianlian.domain.VpnLocalUser;
import com.ruoyi.yianlian.domain.VpnUser;
import com.ruoyi.yianlian.mapper.VpnLocalUserMapper;
import com.ruoyi.yianlian.mapper.VpnUserMapper;
import com.ruoyi.yianlian.service.vpn.IVpnLineAppService;
import com.ruoyi.yianlian.service.vpn.IVpnLocalUserService;
import com.ruoyi.yianlian.utils.AesUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * VPN本地用户 业务层
 */
@Service
public class VpnLocalUserServiceImpl implements IVpnLocalUserService
{
    @Autowired
    private VpnLocalUserMapper localUserMapper;

    @Autowired
    private VpnUserMapper userMapper;

    @Autowired
    private IVpnLineAppService lineAppService;

    @Autowired
    private AesUtils aesUtils;

    @Override
    public List<VpnLocalUser> selectLocalUserList(VpnLocalUser user)
    {
        return localUserMapper.selectLocalUserList(user);
    }

    @Override
    public VpnLocalUser selectLocalUserById(Long localUserId)
    {
        return localUserMapper.selectLocalUserById(localUserId);
    }

    @Override
    public VpnLocalUser selectLocalUserByUserName(String userName)
    {
        return localUserMapper.selectLocalUserByUserName(userName);
    }

    @Override
    public boolean checkUserNameUnique(VpnLocalUser user)
    {
        Long localUserId = user.getLocalUserId() == null ? -1L : user.getLocalUserId();
        VpnLocalUser info = localUserMapper.checkUserNameUnique(user.getUserName());
        if (info != null && !info.getLocalUserId().equals(localUserId))
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public int insertLocalUser(VpnLocalUser user, String plainPassword)
    {
        user.setPassword(SecurityUtils.encryptPassword(plainPassword));
        user.setEncryptedPwd(aesUtils.encrypt(plainPassword));
        return localUserMapper.insertLocalUser(user);
    }

    @Override
    public int updateLocalUser(VpnLocalUser user)
    {
        return localUserMapper.updateLocalUser(user);
    }

    @Override
    public int resetPwd(VpnLocalUser user, String plainPassword)
    {
        user.setPassword(SecurityUtils.encryptPassword(plainPassword));
        user.setEncryptedPwd(aesUtils.encrypt(plainPassword));
        return localUserMapper.resetLocalUserPwd(user);
    }

    @Override
    public int updateLocalUserStatus(VpnLocalUser user)
    {
        return localUserMapper.updateLocalUser(user);
    }

    @Override
    public int deleteLocalUserByIds(Long[] localUserIds)
    {
        for (Long localUserId : localUserIds)
        {
            List<VpnUser> lineUsers = userMapper.selectUsersByLocalUserId(localUserId);
            if (lineUsers != null && !lineUsers.isEmpty())
            {
                throw new ServiceException("本地用户仍存在已同步的线路用户，请先在线路用户管理中处理");
            }
        }
        return localUserMapper.deleteLocalUserByIds(localUserIds);
    }

    @Override
    public void updateLocalUserLogin(VpnLocalUser user)
    {
        localUserMapper.updateLocalUserLogin(user);
    }

    @Override
    public void changeLocalPassword(VpnLocalUser user, String newPassword)
    {
        user.setPassword(SecurityUtils.encryptPassword(newPassword));
        user.setEncryptedPwd(aesUtils.encrypt(newPassword));
        localUserMapper.resetLocalUserPwd(user);
    }

    @Override
    public List<Map<String, Object>> getAuthorizedLines(Long localUserId)
    {
        List<String> appIds = userMapper.selectDistinctAppIdsByLocalUserId(localUserId);
        if (appIds == null || appIds.isEmpty())
        {
            return new ArrayList<>();
        }
        Set<String> appIdSet = new LinkedHashSet<>(appIds);
        List<LineApp> allLines = lineAppService.selectLineAppList(new LineApp());
        return allLines.stream()
            .filter(line -> appIdSet.contains(line.getAppId()) && "0".equals(line.getStatus()))
            .map(this::toLineVo)
            .collect(Collectors.toList());
    }

    @Override
    public boolean isAuthorizedForLine(Long localUserId, String appId)
    {
        if (localUserId == null || StringUtils.isEmpty(appId))
        {
            return false;
        }
        VpnUser user = userMapper.selectUserByLocalUserIdAndAppId(localUserId, appId);
        return user != null && "0".equals(user.getStatus());
    }

    @Override
    public Map<String, String> getLineUserCredentials(Long localUserId, String appId)
    {
        if (!isAuthorizedForLine(localUserId, appId))
        {
            throw new ServiceException("您无权访问所选线路，请联系管理员");
        }
        VpnUser lineUser = userMapper.selectUserByLocalUserIdAndAppId(localUserId, appId);
        if (lineUser == null)
        {
            throw new ServiceException("线路用户不存在");
        }
        String plainPassword = decryptPassword(lineUser.getEncryptedPwd());
        if (StringUtils.isEmpty(plainPassword))
        {
            throw new ServiceException("无法获取线路用户密码，请在线路用户管理中重置密码");
        }
        Map<String, String> credentials = new LinkedHashMap<>();
        credentials.put("username", lineUser.getUserName());
        credentials.put("plainPassword", plainPassword);
        return credentials;
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

    private Map<String, Object> toLineVo(LineApp line)
    {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("appId", line.getAppId());
        vo.put("appName", line.getAppName());
        vo.put("host", line.getHost());
        vo.put("srvPort", line.getSrvPort());
        vo.put("spaPort", line.getSpaPort());
        String spaKey = line.getSpaKey();
        if (spaKey != null && !spaKey.isEmpty())
        {
            spaKey = AesUtils.md5(aesUtils.decrypt(spaKey));
        }
        vo.put("spaKey", spaKey);
        return vo;
    }

    @Override
    public List<VpnLocalUser> selectUnsyncedByAppId(String appId)
    {
        if (StringUtils.isEmpty(appId))
        {
            return new ArrayList<>();
        }
        return localUserMapper.selectUnsyncedByAppId(appId);
    }
}
