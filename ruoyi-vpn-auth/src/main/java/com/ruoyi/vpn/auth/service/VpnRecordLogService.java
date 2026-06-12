package com.ruoyi.vpn.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.constant.Constants;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.utils.ip.IpUtils;
import com.ruoyi.vpn.auth.context.ClientAuditContext;
import com.ruoyi.yianlian.api.RemoteVpnLogininforService;
import com.ruoyi.yianlian.api.domain.VpnLogininfor;

/**
 * 记录 VPN 登录日志
 */
@Component
public class VpnRecordLogService
{
    private static final int MSG_MAX_LEN = 255;

    private static final int CLIENT_OS_MAX_LEN = 128;

    private static final int CLIENT_MAC_MAX_LEN = 32;

    private static final int APP_ID_MAX_LEN = 64;

    private static final int APP_NAME_MAX_LEN = 64;

    /** 登录用途数据库列长 */
    public static final int LOGIN_PURPOSE_DB_MAX_LEN = 100;

    @Autowired
    private RemoteVpnLogininforService remoteVpnLogininforService;

    @Autowired
    private VpnLineAppNameResolver lineAppNameResolver;

    /**
     * 记录登录信息
     *
     * @param username 用户名
     * @param status 状态
     * @param message 消息内容
     * @param loginPurpose 登录用途
     */
    public void recordLogininfor(String username, String status, String message, String loginPurpose)
    {
        VpnLogininfor logininfor = new VpnLogininfor();
        logininfor.setUserName(StringUtils.isNotEmpty(username) ? username : "unknown");
        String auditIp = ClientAuditContext.resolveIpaddr();
        logininfor.setIpaddr(StringUtils.isNotEmpty(auditIp) ? truncateIpaddr(auditIp) : IpUtils.getIpAddr());
        logininfor.setClientOs(truncateClientOs(ClientAuditContext.getClientOs()));
        logininfor.setClientMac(truncateClientMac(ClientAuditContext.getClientMac()));
        logininfor.setMsg(truncateMsg(message));
        logininfor.setLoginPurpose(truncateLoginPurpose(loginPurpose));
        String appId = ClientAuditContext.getAppId();
        logininfor.setAppId(truncateAppId(appId));
        logininfor.setAppName(truncateAppName(
                lineAppNameResolver.resolve(appId, ClientAuditContext.getAppName())));
        if (StringUtils.equalsAny(status, Constants.LOGIN_SUCCESS, Constants.LOGOUT, Constants.REGISTER))
        {
            logininfor.setStatus(Constants.LOGIN_SUCCESS_STATUS);
        }
        else if (Constants.LOGIN_FAIL.equals(status))
        {
            logininfor.setStatus(Constants.LOGIN_FAIL_STATUS);
        }
        else
        {
            logininfor.setStatus(Constants.LOGIN_FAIL_STATUS);
        }
        remoteVpnLogininforService.saveLogininfor(logininfor, SecurityConstants.INNER);
    }

    private String truncateMsg(String message)
    {
        if (message == null)
        {
            return "";
        }
        return message.length() <= MSG_MAX_LEN ? message : message.substring(0, MSG_MAX_LEN);
    }

    private String truncateLoginPurpose(String loginPurpose)
    {
        if (loginPurpose == null)
        {
            return "";
        }
        return loginPurpose.length() <= LOGIN_PURPOSE_DB_MAX_LEN
                ? loginPurpose
                : loginPurpose.substring(0, LOGIN_PURPOSE_DB_MAX_LEN);
    }

    private String truncateIpaddr(String ipaddr)
    {
        if (ipaddr == null)
        {
            return "";
        }
        return ipaddr.length() <= 128 ? ipaddr : ipaddr.substring(0, 128);
    }

    private String truncateClientOs(String clientOs)
    {
        if (clientOs == null)
        {
            return "";
        }
        return clientOs.length() <= CLIENT_OS_MAX_LEN ? clientOs : clientOs.substring(0, CLIENT_OS_MAX_LEN);
    }

    private String truncateClientMac(String clientMac)
    {
        if (clientMac == null)
        {
            return "";
        }
        return clientMac.length() <= CLIENT_MAC_MAX_LEN ? clientMac : clientMac.substring(0, CLIENT_MAC_MAX_LEN);
    }

    private String truncateAppId(String appId)
    {
        if (appId == null)
        {
            return "";
        }
        return appId.length() <= APP_ID_MAX_LEN ? appId : appId.substring(0, APP_ID_MAX_LEN);
    }

    private String truncateAppName(String appName)
    {
        if (appName == null)
        {
            return "";
        }
        return appName.length() <= APP_NAME_MAX_LEN ? appName : appName.substring(0, APP_NAME_MAX_LEN);
    }
}
