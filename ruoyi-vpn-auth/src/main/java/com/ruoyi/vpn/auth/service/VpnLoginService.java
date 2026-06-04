package com.ruoyi.vpn.auth.service;

import com.ruoyi.yianlian.api.domain.VpnChangePasswordRequest;
import com.ruoyi.yianlian.api.domain.VpnUserInfo;
import com.ruoyi.yianlian.api.model.VpnLoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.constant.CacheConstants;
import com.ruoyi.common.core.constant.Constants;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.constant.UserConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.enums.UserStatus;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.utils.ip.IpUtils;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.api.RemoteVpnLineService;
import com.ruoyi.yianlian.api.RemoteVpnUserService;

/**
 * 登录校验方法
 * 
 * @author ruoyi
 */
@Component
public class VpnLoginService
{
    @Autowired
    private RemoteVpnUserService remoteVpnUserService;

    @Autowired
    private RemoteVpnLineService remoteVpnLineService;

    @Autowired
    private VpnPasswordService passwordService;

    @Autowired
    private VpnRecordLogService recordLogService;

    @Autowired
    private RedisService redisService;

    /**
     * 登录
     */
    public VpnLoginUser login(String username, String password, String appId)
    {
        if (StringUtils.isEmpty(appId))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "未选择线路");
            throw new ServiceException("请先选择线路");
        }
        // 用户名或密码为空 错误
        if (StringUtils.isAnyBlank(username, password))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户/密码必须填写");
            throw new ServiceException("用户/密码必须填写");
        }
        // 密码如果不在指定范围内 错误
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户密码不在指定范围");
            throw new ServiceException("用户密码不在指定范围");
        }
        // 用户名不在指定范围内 错误
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户名不在指定范围");
            throw new ServiceException("用户名不在指定范围");
        }
        // IP黑名单校验
        String blackStr = Convert.toStr(redisService.getCacheObject(CacheConstants.SYS_LOGIN_BLACKIPLIST));
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "很遗憾，访问IP已被列入系统黑名单");
            throw new ServiceException("很遗憾，访问IP已被列入系统黑名单");
        }
        // 查询用户信息（按所选线路）
        R<VpnLoginUser> userResult = remoteVpnUserService.getUserInfo(username, appId, SecurityConstants.INNER);

        if (R.FAIL == userResult.getCode())
        {
            throw new ServiceException(userResult.getMsg());
        }

        VpnLoginUser userInfo = userResult.getData();
        VpnUserInfo user = userResult.getData().getVpnUser();
        if (UserStatus.DELETED.getCode().equals(user.getDelFlag()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "对不起，您的账号已被删除");
            throw new ServiceException("对不起，您的账号：" + username + " 已被删除");
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户已停用，请联系管理员");
            throw new ServiceException("对不起，您的账号：" + username + " 已停用");
        }
        passwordService.validate(user, password);
        assertUserAuthorizedForLine(user.getUserId(), appId);
        recordLogService.recordLogininfor(username, Constants.LOGIN_SUCCESS, "登录成功");
        recordLoginInfo(user.getUserId());
        // 缓存明文密码，用于后续控制器登录（有效期与token一致，30分钟）
        redisService.setCacheObject("vpn_plain_pwd:" + user.getUserId(), password, 30L, java.util.concurrent.TimeUnit.MINUTES);
        return userInfo;
    }

    /**
     * 记录登录信息
     *
     * @param userId 用户ID
     */
    public void recordLoginInfo(Long userId)
    {
        VpnUserInfo vpnUser = new VpnUserInfo();
        vpnUser.setUserId(userId);
        // 更新用户登录IP
        vpnUser.setLoginIp(IpUtils.getIpAddr());
        // 更新用户登录时间
        vpnUser.setLoginDate(DateUtils.getNowDate());
        remoteVpnUserService.recordUserLogin(vpnUser, SecurityConstants.INNER);
    }

    /**
     * 退出
     */
    public void logout(String loginName)
    {
        recordLogService.recordLogininfor(loginName, Constants.LOGOUT, "退出成功");
    }

    /**
     * 解锁
     */
    public void unlock(String password)
    {
        String username = SecurityUtils.getUsername();
        // 或密码为空 错误
        if (StringUtils.isEmpty(password))
        {
            throw new ServiceException("密码不能为空");
        }
        // 查询用户信息（解锁场景不按线路隔离）
        R<VpnLoginUser> userResult = remoteVpnUserService.getUserInfo(username, null, SecurityConstants.INNER);

        if (R.FAIL == userResult.getCode())
        {
            throw new ServiceException(userResult.getMsg());
        }

        VpnUserInfo user = userResult.getData().getVpnUser();
        if (!SecurityUtils.matchesPassword(password, user.getPassword()))
        {
            throw new ServiceException("密码错误，请重新输入");
        }
    }

    /**
     * 修改密码
     */
    public void changePassword(String username, String oldPassword, String newPassword, String appId)
    {
        // 用户名或密码为空 错误
        if (StringUtils.isAnyBlank(username, oldPassword, newPassword))
        {
            throw new ServiceException("用户名/旧密码/新密码必须填写");
        }
        if (StringUtils.isEmpty(appId))
        {
            throw new ServiceException("请先选择线路");
        }
        // 新旧密码不能相同
        if (oldPassword.equals(newPassword))
        {
            throw new ServiceException("新密码不能与旧密码相同");
        }
        // 密码如果不在指定范围内 错误
        if (newPassword.length() < UserConstants.PASSWORD_MIN_LENGTH
                || newPassword.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            throw new ServiceException("新密码长度不在指定范围");
        }
        // 调用yianlian模块执行修改密码
        VpnChangePasswordRequest request = new VpnChangePasswordRequest();
        request.setUsername(username);
        request.setOldPassword(oldPassword);
        request.setNewPassword(newPassword);
        request.setAppId(appId);
        R<Boolean> result = remoteVpnUserService.changePassword(request, SecurityConstants.INNER);
        if (R.FAIL == result.getCode())
        {
            throw new ServiceException(result.getMsg());
        }
    }

    /**
     * 校验用户是否拥有所选线路的访问授权
     */
    private void assertUserAuthorizedForLine(Long userId, String appId)
    {
        R<java.util.List<java.util.Map<String, Object>>> linesResult =
                remoteVpnUserService.getAuthorizedLines(userId, SecurityConstants.INNER);
        if (R.FAIL == linesResult.getCode() || linesResult.getData() == null)
        {
            throw new ServiceException("获取线路授权失败");
        }
        boolean allowed = linesResult.getData().stream()
                .anyMatch(m -> appId.equals(m.get("appId")));
        if (!allowed)
        {
            throw new ServiceException("您无权访问所选线路，请联系管理员");
        }
    }

    /**
     * 登录前可选线路列表
     */
    public java.util.List<java.util.Map<String, Object>> listPublicLines()
    {
        R<java.util.List<java.util.Map<String, Object>>> result =
                remoteVpnLineService.listPublicLines(SecurityConstants.INNER);
        if (R.FAIL == result.getCode() || result.getData() == null)
        {
            throw new ServiceException(StringUtils.isNotEmpty(result.getMsg()) ? result.getMsg() : "获取线路列表失败");
        }
        return result.getData();
    }

}
