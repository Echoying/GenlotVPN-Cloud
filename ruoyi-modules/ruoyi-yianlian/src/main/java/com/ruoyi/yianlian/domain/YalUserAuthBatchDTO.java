package com.ruoyi.yianlian.domain;

import java.util.List;

/**
 * 用户授权批量保存DTO
 *
 * @author ruoyi
 */
public class YalUserAuthBatchDTO
{
    /** 用户ID */
    private Long userId;

    /** 授权列表 */
    private List<YalUserAuth> authList;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public List<YalUserAuth> getAuthList()
    {
      return authList;
    }

    public void setAuthList(List<YalUserAuth> authList)
    {
        this.authList = authList;
    }
}
