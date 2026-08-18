package com.ruoyi.yianlian.domain.vo;

import java.util.ArrayList;
import java.util.List;

public class VpnLineAuthLineView
{
    private String lineId;
    private String lineName;
    private List<VpnLineAuthItemView> items = new ArrayList<VpnLineAuthItemView>();

    public String getLineId()
    {
        return lineId;
    }

    public void setLineId(String lineId)
    {
        this.lineId = lineId;
    }

    public String getLineName()
    {
        return lineName;
    }

    public void setLineName(String lineName)
    {
        this.lineName = lineName;
    }

    public List<VpnLineAuthItemView> getItems()
    {
        return items;
    }

    public void setItems(List<VpnLineAuthItemView> items)
    {
        this.items = items;
    }
}
