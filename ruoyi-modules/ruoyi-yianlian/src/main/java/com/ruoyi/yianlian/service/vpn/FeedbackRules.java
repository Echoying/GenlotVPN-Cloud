package com.ruoyi.yianlian.service.vpn;

import com.ruoyi.common.core.exception.ServiceException;

/**
 * VPN 问题反馈字段校验规则。
 */
public final class FeedbackRules
{
    private static final int MAX_TITLE_LENGTH = 80;
    private static final int MAX_CONTENT_LENGTH = 2000;

    private FeedbackRules() {}

    public static String normalizeCategory(String raw)
    {
        if (raw == null)
        {
            return "";
        }
        String trimmed = raw.trim();
        if ("connect".equals(trimmed) || "login".equals(trimmed)
                || "ui".equals(trimmed) || "other".equals(trimmed))
        {
            return trimmed;
        }
        return "";
    }

    public static boolean isValidStatus(String status)
    {
        return "0".equals(status) || "1".equals(status) || "2".equals(status);
    }

    public static void assertTitle(String title)
    {
        if (title == null || title.trim().isEmpty())
        {
            throw new ServiceException("请填写标题");
        }
        if (title.trim().length() > MAX_TITLE_LENGTH)
        {
            throw new ServiceException("标题过长");
        }
    }

    public static void assertContent(String content)
    {
        if (content == null || content.trim().isEmpty())
        {
            throw new ServiceException("请填写描述");
        }
        if (content.trim().length() > MAX_CONTENT_LENGTH)
        {
            throw new ServiceException("描述过长");
        }
    }
}
