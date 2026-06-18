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
import com.ruoyi.yianlian.api.RemoteVpnLocalUserService;

import java.util.Map;

/**
 * 登录校验方法
 * 
 * @author ruoyi
 */
@Component
public class VpnLoginService
{
    @Autowired
    private RemoteVpnLocalUserService remoteVpnLocalUserService;

    @Autowired
    private RemoteVpnLineService remoteVpnLineService;

    @Autowired
    private VpnPasswordService passwordService;

    @Autowired
    private VpnRecordLogService recordLogService;

    @Autowired
    private RedisService redisService;

    /**
     * 本地用户登录（无需 appId）
     */
    public VpnLoginUser login(String username, String password, String appId, String loginPurpose)
    {
        if (StringUtils.isAnyBlank(username, password))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户/密码必须填写", loginPurpose);
            throw new ServiceException("用户/密码必须填写");
        }
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户密码不在指定范围", loginPurpose);
            throw new ServiceException("用户密码不在指定范围");
        }
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户名不在指定范围", loginPurpose);
            throw new ServiceException("用户名不在指定范围");
        }
        String blackStr = Convert.toStr(redisService.getCacheObject(CacheConstants.SYS_LOGIN_BLACKIPLIST));
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "很遗憾，访问IP已被列入系统黑名单", loginPurpose);
            throw new ServiceException("很遗憾，访问IP已被列入系统黑名单");
        }

        R<VpnLoginUser> userResult = remoteVpnLocalUserService.getUserInfo(username, SecurityConstants.INNER);
        if (R.FAIL == userResult.getCode())
        {
            throw new ServiceException(userResult.getMsg());
        }

        VpnLoginUser userInfo = userResult.getData();
        VpnUserInfo user = userInfo.getVpnUser();
        if (UserStatus.DELETED.getCode().equals(user.getDelFlag()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "对不起，您的账号已被删除", loginPurpose);
            throw new ServiceException("对不起，您的账号：" + username + " 已被删除");
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户已停用，请联系管理员", loginPurpose);
            throw new ServiceException("对不起，您的账号：" + username + " 已停用");
        }
        passwordService.validate(user, password, loginPurpose);
        recordLoginInfo(user.getUserId());
        return userInfo;
    }

    /**
     * 记录登录信息
     */
    public void recordLoginInfo(Long localUserId)
    {
        VpnUserInfo vpnUser = new VpnUserInfo();
        vpnUser.setUserId(localUserId);
        vpnUser.setLoginIp(IpUtils.getIpAddr());
        vpnUser.setLoginDate(DateUtils.getNowDate());
        remoteVpnLocalUserService.recordUserLogin(vpnUser, SecurityConstants.INNER);
    }

    /**
     * 退出
     */
    public void logout(String loginName)
    {
        recordLogService.recordLogininfor(loginName, Constants.LOGOUT, "退出成功", "");
    }

    /**
     * 解锁
     */
    public void unlock(String password)
    {
        String username = SecurityUtils.getUsername();
        if (StringUtils.isEmpty(password))
        {
            throw new ServiceException("密码不能为空");
        }
        R<VpnLoginUser> userResult = remoteVpnLocalUserService.getUserInfo(username, SecurityConstants.INNER);
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
     * 修改本地用户密码
     */
    public void changePassword(String username, String oldPassword, String newPassword, String appId)
    {
        if (StringUtils.isAnyBlank(username, oldPassword, newPassword))
        {
            throw new ServiceException("用户名/旧密码/新密码必须填写");
        }
        if (oldPassword.equals(newPassword))
        {
            throw new ServiceException("新密码不能与旧密码相同");
        }
        if (newPassword.length() < UserConstants.PASSWORD_MIN_LENGTH
                || newPassword.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            throw new ServiceException("新密码长度不在指定范围");
        }
        VpnChangePasswordRequest request = new VpnChangePasswordRequest();
        request.setUsername(username);
        request.setOldPassword(oldPassword);
        request.setNewPassword(newPassword);
        R<Boolean> result = remoteVpnLocalUserService.changePassword(request, SecurityConstants.INNER);
        if (R.FAIL == result.getCode())
        {
            throw new ServiceException(result.getMsg());
        }
    }

    /**
     * 校验本地用户是否拥有所选线路的访问授权
     */
    public void assertLocalUserAuthorizedForLine(Long localUserId, String appId)
    {
        R<Boolean> result = remoteVpnLocalUserService.isAuthorizedForLine(localUserId, appId, SecurityConstants.INNER);
        if (R.FAIL == result.getCode() || result.getData() == null || !result.getData())
        {
            throw new ServiceException("您无权访问所选线路，请联系管理员");
        }
    }

    /**
     * 获取本地用户授权线路列表
     */
    public java.util.List<java.util.Map<String, Object>> getAuthorizedLines(Long localUserId)
    {
        R<java.util.List<java.util.Map<String, Object>>> result =
                remoteVpnLocalUserService.getAuthorizedLines(localUserId, SecurityConstants.INNER);
        if (R.FAIL == result.getCode() || result.getData() == null)
        {
            throw new ServiceException(StringUtils.isNotEmpty(result.getMsg()) ? result.getMsg() : "获取授权线路失败");
        }
        return result.getData();
    }

    /**
     * 获取线路用户控制器登录凭证（用户名 + 明文密码）
     */
    public Map<String, String> getLineUserCredentials(Long localUserId, String appId)
    {
        R<Map<String, String>> result = remoteVpnLocalUserService.getLineUserCredentials(
                localUserId, appId, SecurityConstants.INNER);
        if (R.FAIL == result.getCode() || result.getData() == null)
        {
            throw new ServiceException(StringUtils.isNotEmpty(result.getMsg()) ? result.getMsg() : "获取线路用户凭证失败");
        }
        return result.getData();
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
