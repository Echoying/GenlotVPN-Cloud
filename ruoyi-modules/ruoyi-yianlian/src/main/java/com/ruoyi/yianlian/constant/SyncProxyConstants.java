package com.ruoyi.yianlian.constant;

import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.domain.LineApp;
import com.ruoyi.yianlian.domain.VpnRole;

/**
 * 同步代理相关常量
 */
public final class SyncProxyConstants
{
    private SyncProxyConstants()
    {
    }

    /** VPN 角色权限字符：同步代理管理员 */
    public static final String ROLE_SYNC_PROXY = "sync_proxy";

    /** 线路启用同步代理 */
    public static final String PROXY_ENABLED_YES = "1";

    /** 线路直连（不走同步代理） */
    public static final String PROXY_ENABLED_NO = "0";

    public static boolean isLineProxyEnabled(LineApp lineApp)
    {
        return lineApp != null && PROXY_ENABLED_YES.equals(lineApp.getProxyEnabled());
    }

    /**
     * 解析角色权限字符（与登录下发 role_keys 规则一致）
     */
    public static String resolveRoleKey(VpnRole role)
    {
        if (role == null)
        {
            return "";
        }
        return StringUtils.isNotEmpty(role.getRoleKey())
            ? role.getRoleKey().trim()
            : StringUtils.trimToEmpty(role.getRoleName());
    }

    /**
     * 是否为当前线路的同步代理管理员角色
     */
    public static boolean matchesSyncProxyRole(VpnRole role, String appId)
    {
        if (role == null || StringUtils.isEmpty(appId))
        {
            return false;
        }
        return ROLE_SYNC_PROXY.equals(resolveRoleKey(role))
            && appId.equals(StringUtils.trimToEmpty(role.getAppId()));
    }
}
