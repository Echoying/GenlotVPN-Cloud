package com.ruoyi.yianlian.domain.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户线路权限只读汇总
 */
public class VpnLineAuthView
{
    private Long userId;
    private String userName;
    private String userType;
    private List<VpnLineAuthLineView> lines = new ArrayList<VpnLineAuthLineView>();

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getUserType()
    {
        return userType;
    }

    public void setUserType(String userType)
    {
        this.userType = userType;
    }

    public List<VpnLineAuthLineView> getLines()
    {
        return lines;
    }

    public void setLines(List<VpnLineAuthLineView> lines)
    {
        this.lines = lines;
    }
}
