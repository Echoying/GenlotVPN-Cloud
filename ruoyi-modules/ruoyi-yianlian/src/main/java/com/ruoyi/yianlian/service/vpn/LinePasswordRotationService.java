package com.ruoyi.yianlian.service.vpn;

import com.ruoyi.common.core.enums.UserStatus;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.domain.VpnUser;
import com.ruoyi.yianlian.mapper.VpnUserMapper;
import com.ruoyi.yianlian.utils.AesUtils;
import org.springframework.stereotype.Service;

/**
 * 当前授权线路的密码轮换服务。
 */
@Service
public class LinePasswordRotationService
{
    private static final int MAX_GENERATE_ATTEMPTS = 8;
    private static final String LINE_FORBIDDEN_MESSAGE = "您无权访问所选线路，请联系管理员";
    private static final String PASSWORD_UNAVAILABLE_MESSAGE =
        "无法获取线路用户密码，请在线路用户管理中重置密码";
    /** 等前一次轮换出结果的最长时间，须小于调用方 Feign 读超时 */
    private static final long LOCK_WAIT_MS = 45_000L;

    private final VpnUserMapper userMapper;
    private final IVpnLocalUserService localUserService;
    private final IVpnUserService userService;
    private final AesUtils aesUtils;
    private final LinePasswordGenerator passwordGenerator;
    private final LinePasswordRotationGuard rotationGuard;

    public LinePasswordRotationService(VpnUserMapper userMapper,
                                       IVpnLocalUserService localUserService,
                                       IVpnUserService userService,
                                       AesUtils aesUtils,
                                       LinePasswordGenerator passwordGenerator,
                                       LinePasswordRotationGuard rotationGuard)
    {
        this.userMapper = userMapper;
        this.localUserService = localUserService;
        this.userService = userService;
        this.aesUtils = aesUtils;
        this.passwordGenerator = passwordGenerator;
        this.rotationGuard = rotationGuard;
    }

    public void rotate(Long localUserId, String appId)
    {
        if (localUserId == null || StringUtils.isEmpty(appId))
        {
            throw new ServiceException("本地用户ID和线路ID不能为空");
        }
        if (!localUserService.isAuthorizedForLine(localUserId, appId))
        {
            throw new ServiceException(LINE_FORBIDDEN_MESSAGE);
        }
        // 调用方超时重试时，前一次可能已经改密成功，直接复用结果
        if (rotationGuard.isRotatedRecently(localUserId, appId))
        {
            return;
        }

        String lockToken = rotationGuard.tryAcquire(localUserId, appId, LOCK_WAIT_MS);
        if (lockToken == null)
        {
            throw new ServiceException("线路密码正在更新，请稍后重试");
        }
        try
        {
            // 等锁期间前一次已成功，不再重复改密
            if (rotationGuard.isRotatedRecently(localUserId, appId))
            {
                return;
            }
            doRotate(localUserId, appId);
            rotationGuard.markRotated(localUserId, appId);
        }
        finally
        {
            rotationGuard.release(localUserId, appId, lockToken);
        }
    }

    private void doRotate(Long localUserId, String appId)
    {
        // 线路用户不存在、停用或已删除时沿用授权失败文案，不暴露线路用户状态
        VpnUser lineUser = userMapper.selectUserByLocalUserIdAndAppId(localUserId, appId);
        if (lineUser == null
            || UserStatus.DISABLE.getCode().equals(lineUser.getStatus())
            || UserStatus.DELETED.getCode().equals(lineUser.getDelFlag()))
        {
            throw new ServiceException(LINE_FORBIDDEN_MESSAGE);
        }

        String oldPlainPassword = decryptPassword(lineUser.getEncryptedPwd());
        if (StringUtils.isEmpty(oldPlainPassword))
        {
            throw new ServiceException(PASSWORD_UNAVAILABLE_MESSAGE);
        }

        String newPlainPassword = generateDifferentPassword(oldPlainPassword);
        // 只带主键和密码字段，避免 updateUser 把查询实体上的旧字段一并写回
        VpnUser passwordUpdate = new VpnUser(lineUser.getUserId());
        passwordUpdate.setPassword(SecurityUtils.encryptPassword(newPlainPassword));
        passwordUpdate.setEncryptedPwd(aesUtils.encrypt(newPlainPassword));
        if (userService.resetPwdWithSync(passwordUpdate, newPlainPassword) <= 0)
        {
            throw new ServiceException("更新线路密码失败，请重试");
        }
    }

    private String decryptPassword(String encryptedPassword)
    {
        if (StringUtils.isEmpty(encryptedPassword))
        {
            return null;
        }
        try
        {
            return aesUtils.decrypt(encryptedPassword);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private String generateDifferentPassword(String oldPlainPassword)
    {
        for (int i = 0; i < MAX_GENERATE_ATTEMPTS; i++)
        {
            String candidate = passwordGenerator.generate();
            if (!oldPlainPassword.equals(candidate))
            {
                return candidate;
            }
        }
        throw new ServiceException("生成线路密码失败，请重试");
    }
}
