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

    /** 线路ID（line_app.app_id） */
    private String lineId;

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

    public String getLineId()
    {
        return lineId;
    }

    public void setLineId(String lineId)
    {
        this.lineId = lineId;
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
