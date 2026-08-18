package com.ruoyi.yianlian.service.vpn;

import com.ruoyi.common.core.exception.ServiceException;

/**
 * VPN 问题反馈图片校验规则。
 */
public final class FeedbackImageRules
{
    public static final int MAX_BYTES = 400 * 1024;

    private FeedbackImageRules() {}

    public static String detectContentType(byte[] image)
    {
        if (image == null || image.length < 3)
        {
            return null;
        }
        if ((image[0] & 0xFF) == 0xFF && (image[1] & 0xFF) == 0xD8 && (image[2] & 0xFF) == 0xFF)
        {
            return "image/jpeg";
        }
        if (image.length >= 4
                && (image[0] & 0xFF) == 0x89 && (image[1] & 0xFF) == 0x50
                && (image[2] & 0xFF) == 0x4E && (image[3] & 0xFF) == 0x47)
        {
            return "image/png";
        }
        return null;
    }

    public static void assertUpload(byte[] image)
    {
        if (image == null || image.length == 0 || image.length > MAX_BYTES
                || detectContentType(image) == null)
        {
            throw new ServiceException("图片过大或格式不支持");
        }
    }
}
