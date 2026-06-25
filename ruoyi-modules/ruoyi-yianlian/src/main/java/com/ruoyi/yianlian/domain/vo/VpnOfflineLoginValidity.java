package com.ruoyi.yianlian.domain.vo;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 离线登录有效时间选项
 */
public enum VpnOfflineLoginValidity
{
    H6("H6", "6个小时", 6, ChronoUnit.HOURS),
    D1("D1", "一天", 1, ChronoUnit.DAYS),
    D3("D3", "三天", 3, ChronoUnit.DAYS),
    W1("W1", "一个星期", 7, ChronoUnit.DAYS),
    M1("M1", "一个月", 30, ChronoUnit.DAYS),
    Y1("Y1", "一年", 365, ChronoUnit.DAYS);

    private final String code;
    private final String label;
    private final long amount;
    private final ChronoUnit unit;

    VpnOfflineLoginValidity(String code, String label, long amount, ChronoUnit unit)
    {
        this.code = code;
        this.label = label;
        this.amount = amount;
        this.unit = unit;
    }

    public String getCode()
    {
        return code;
    }

    public String getLabel()
    {
        return label;
    }

    public LocalDateTime resolveExpireAt(LocalDateTime base)
    {
        return base.plus(amount, unit);
    }

    public static VpnOfflineLoginValidity fromCode(String code)
    {
        for (VpnOfflineLoginValidity value : values())
        {
            if (value.code.equals(code))
            {
                return value;
            }
        }
        throw new IllegalArgumentException("无效的有效时间选项: " + code);
    }
}
