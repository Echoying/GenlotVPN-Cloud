package com.ruoyi.yianlian.service.vpn;

import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.client.dto.YiAnLianUserCreateResultItem;
import com.ruoyi.yianlian.client.dto.YiAnLianUserPasswordResetRequest;
import com.ruoyi.yianlian.client.dto.vo.YiAnLianUserVO;
import com.ruoyi.yianlian.domain.VpnDeptYianlianMapping;
import com.ruoyi.yianlian.domain.VpnUser;
import com.ruoyi.yianlian.domain.VpnUserYianlianMapping;
import com.ruoyi.yianlian.service.IVpnDeptYianlianMappingService;
import com.ruoyi.yianlian.service.vpn.IVpnUserYianlianMappingService;
import com.ruoyi.yianlian.service.yianlian.IYiAnLianUserService;
import com.ruoyi.yianlian.utils.AesUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * VPN 用户同步易安联（返回 boolean，由调用方决定事务回滚）
 */
@Service
public class VpnUserYiAnLianSyncService
{
    @Autowired
    private IYiAnLianUserService yiAnLianUserService;

    @Autowired
    private IVpnUserYianlianMappingService userMappingService;

    @Autowired
    private IVpnDeptYianlianMappingService deptMappingService;

    @Autowired
    private AesUtils aesUtils;

    /**
     * 新增后同步到易安联
     */
    public boolean syncOnAdd(VpnUser user, String plainPassword)
    {
        String appId = user.getAppId();
        if (StringUtils.isEmpty(appId))
        {
            return true;
        }
        String yiAnLianDeptId = resolveYiAnLianDeptId(user.getDeptId(), appId);
        YiAnLianUserVO yiAnLianUser = buildYiAnLianUserVO(user, yiAnLianDeptId);
        yiAnLianUser.setPassword(plainPassword);

        List<YiAnLianUserCreateResultItem> results = yiAnLianUserService.create(appId, Collections.singletonList(yiAnLianUser));
        if (results == null || results.isEmpty())
        {
            return false;
        }
        YiAnLianUserCreateResultItem resultItem = results.get(0);
        if (resultItem.getData() == null || resultItem.getData().getId() == null)
        {
            return false;
        }
        return saveMapping(user.getUserId(), appId, resultItem.getData().getId());
    }

    /**
     * 修改后同步到易安联
     */
    public boolean syncOnEdit(VpnUser user)
    {
        String appId = user.getAppId();
        if (StringUtils.isEmpty(appId))
        {
            return true;
        }
        VpnUserYianlianMapping mapping = userMappingService.selectByUserIdAndAppId(user.getUserId(), appId);
        if (mapping == null || StringUtils.isEmpty(mapping.getYianlianId()))
        {
            return false;
        }
        String yiAnLianDeptId = resolveYiAnLianDeptId(user.getDeptId(), appId);
        YiAnLianUserVO remoteUser = buildYiAnLianUserVO(user, yiAnLianDeptId);
        remoteUser.setId(mapping.getYianlianId());
        return Boolean.TRUE.equals(yiAnLianUserService.update(appId, remoteUser));
    }

    /**
     * 删除前同步易安联（无 mapping 视为成功）
     */
    public boolean syncOnDelete(VpnUserYianlianMapping mapping)
    {
        if (mapping == null)
        {
            return true;
        }
        if (StringUtils.isEmpty(mapping.getYianlianId()))
        {
            return false;
        }
        return Boolean.TRUE.equals(yiAnLianUserService.delete(mapping.getAppId(),
                Collections.singletonList(mapping.getYianlianId())));
    }

    /**
     * 重置密码同步易安联
     */
    public boolean syncOnResetPassword(VpnUser user, String plainPassword, String oldEncryptedPwd)
    {
        String appId = user.getAppId();
        if (StringUtils.isEmpty(appId))
        {
            return true;
        }
        VpnUserYianlianMapping mapping = userMappingService.selectByUserIdAndAppId(user.getUserId(), appId);
        if (mapping == null)
        {
            return false;
        }
        String oldPassword = decryptOldPassword(oldEncryptedPwd);
        YiAnLianUserPasswordResetRequest resetRequest = new YiAnLianUserPasswordResetRequest();
        resetRequest.setAppId(appId);
        resetRequest.setUsername(user.getUserName());
        resetRequest.setOldPassword(oldPassword);
        resetRequest.setNewPassword(plainPassword);
        return Boolean.TRUE.equals(yiAnLianUserService.resetPassword(appId, resetRequest));
    }

    /**
     * 修改状态同步易安联
     */
    public boolean syncOnChangeStatus(VpnUser user, String status)
    {
        String appId = user.getAppId();
        if (StringUtils.isEmpty(appId))
        {
            return true;
        }
        VpnUserYianlianMapping mapping = userMappingService.selectByUserIdAndAppId(user.getUserId(), appId);
        if (mapping == null || StringUtils.isEmpty(mapping.getYianlianId()))
        {
            return false;
        }
        YiAnLianUserVO remoteUser = new YiAnLianUserVO();
        remoteUser.setId(mapping.getYianlianId());
        remoteUser.setUsername(user.getUserName());
        remoteUser.setName(user.getNickName());
        remoteUser.setStatus("0".equals(status) ? "enable" : "disable");
        return Boolean.TRUE.equals(yiAnLianUserService.update(appId, remoteUser));
    }

    private String resolveYiAnLianDeptId(Long deptId, String appId)
    {
        if (deptId == null)
        {
            return null;
        }
        VpnDeptYianlianMapping deptMapping = deptMappingService.selectByDeptIdAndAppId(deptId, appId);
        return deptMapping != null ? deptMapping.getYianlianId() : null;
    }

    private YiAnLianUserVO buildYiAnLianUserVO(VpnUser user, String yiAnLianDeptId)
    {
        YiAnLianUserVO vo = new YiAnLianUserVO();
        vo.setUsername(user.getUserName());
        vo.setName(user.getNickName());
        vo.setMobile(user.getPhonenumber());
        vo.setEmail(user.getEmail());
        vo.setGender(user.getSex());
        vo.setStatus("0".equals(user.getStatus()) ? "enable" : "disable");
        if (yiAnLianDeptId != null)
        {
            vo.setGroups(Collections.singletonList(yiAnLianDeptId));
        }
        return vo;
    }

    private String decryptOldPassword(String oldEncryptedPwd)
    {
        if (StringUtils.isEmpty(oldEncryptedPwd))
        {
            return null;
        }
        try
        {
            return aesUtils.decrypt(oldEncryptedPwd);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private boolean saveMapping(Long userId, String appId, String yianlianId)
    {
        if (StringUtils.isEmpty(yianlianId))
        {
            return false;
        }
        VpnUserYianlianMapping mapping = new VpnUserYianlianMapping();
        mapping.setUserId(userId);
        mapping.setAppId(appId);
        mapping.setYianlianId(yianlianId);
        mapping.setCreateTime(new Date());
        userMappingService.insert(mapping);
        return true;
    }
}
