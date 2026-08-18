package com.ruoyi.vpn.auth.util;

import java.util.regex.Pattern;

/**
 * 客户端版本号比较工具（x.y.z，按数字段比较）。
 * 与 yianlian 侧算法保持一致；放在 vpn-auth 内避免依赖实现模块。
 */
public final class ClientVersionComparator
{
    private static final Pattern SEMVER = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");

    private ClientVersionComparator() {}

    public static boolean isValid(String version)
    {
        return version != null && SEMVER.matcher(version.trim()).matches();
    }

    public static int compare(String a, String b)
    {
        if (!isValid(a) || !isValid(b))
        {
            throw new IllegalArgumentException("版本号必须为 x.y.z");
        }
        String[] pa = a.trim().split("\\.");
        String[] pb = b.trim().split("\\.");
        for (int i = 0; i < 3; i++)
        {
            int diff = Integer.parseInt(pa[i]) - Integer.parseInt(pb[i]);
            if (diff != 0)
            {
                return diff < 0 ? -1 : 1;
            }
        }
        return 0;
    }

    public static boolean isLower(String client, String min)
    {
        return compare(client, min) < 0;
    }
}
