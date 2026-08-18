package com.ruoyi.yianlian.service.vpn;

import com.ruoyi.common.core.utils.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 线路权限查看来源合并（无 Spring，便于单测）
 */
public final class LineAuthMerger
{
    private LineAuthMerger()
    {
    }

    public static List<String> splitIds(String csv)
    {
        List<String> ids = new ArrayList<String>();
        if (StringUtils.isEmpty(csv))
        {
            return ids;
        }
        String[] parts = csv.split(",");
        for (int i = 0; i < parts.length; i++)
        {
            String part = parts[i] == null ? "" : parts[i].trim();
            if (StringUtils.isNotEmpty(part))
            {
                ids.add(part);
            }
        }
        return ids;
    }

    public static String itemKey(String appGroupId, String appId)
    {
        String g = appGroupId == null ? "" : appGroupId;
        String a = appId == null ? "" : appId;
        return g + "|" + a;
    }

    public static void mergeSource(Map<String, LinkedHashSet<String>> acc, String key, String source)
    {
        LinkedHashSet<String> set = acc.get(key);
        if (set == null)
        {
            set = new LinkedHashSet<String>();
            acc.put(key, set);
        }
        set.add(source);
    }

    public static List<String> sourcesOf(Map<String, LinkedHashSet<String>> acc, String key)
    {
        LinkedHashSet<String> set = acc.get(key);
        return set == null ? new ArrayList<String>() : new ArrayList<String>(set);
    }
}
