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

    /** VPN 角色权限字符：同步代理管理员（VPN 桌面端拉取代理配置时校验，服务端同步 login 已固定用 {@link #LOGIN_USERNAME}） */
    public static final String ROLE_SYNC_PROXY = "sync_proxy";

    /** 服务端代理同步 login 固定使用的线路 VPN 用户名 */
    public static final String LOGIN_USERNAME = "system";

    /** 线路启用同步代理 */
    public static final String PROXY_ENABLED_YES = "1";

    /** 线路直连（不走同步代理） */
    public static final String PROXY_ENABLED_NO = "0";

    /** 代理会话分布式锁 Redis 键前缀（后接 adminKey） */
    public static final String SESSION_LOCK_KEY_PREFIX = "yianlian:sync-proxy:lock:";

    /** 代理会话活跃标记 Redis 键前缀（后接 adminKey），供线路探测跳过 */
    public static final String SESSION_ACTIVE_KEY_PREFIX = "yianlian:sync-proxy:active:";

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
