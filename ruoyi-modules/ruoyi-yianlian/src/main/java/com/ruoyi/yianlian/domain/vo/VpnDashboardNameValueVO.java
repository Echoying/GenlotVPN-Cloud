package com.ruoyi.yianlian.domain.vo;

import java.io.Serializable;

/**
 * 仪表盘名称-数值项
 */
public class VpnDashboardNameValueVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String name;

    private Long value;

    public VpnDashboardNameValueVO()
    {
    }

    public VpnDashboardNameValueVO(String name, Long value)
    {
        this.name = name;
        this.value = value;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Long getValue()
    {
        return value;
    }

    public void setValue(Long value)
    {
        this.value = value;
    }
}
