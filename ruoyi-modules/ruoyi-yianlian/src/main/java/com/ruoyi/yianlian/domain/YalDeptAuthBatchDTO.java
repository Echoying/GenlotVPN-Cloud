package com.ruoyi.yianlian.domain;

import java.util.List;

/**
 * 部门授权批量保存DTO
 *
 * @author ruoyi
 */
public class YalDeptAuthBatchDTO
{
    /** 部门ID */
    private Long deptId;

    /** 线路ID（line_app.app_id） */
    private String lineId;

    /** 授权列表 */
    private List<YalDeptAuth> authList;

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public String getLineId()
    {
        return lineId;
    }

    public void setLineId(String lineId)
    {
        this.lineId = lineId;
    }

    public List<YalDeptAuth> getAuthList()
    {
        return authList;
    }

    public void setAuthList(List<YalDeptAuth> authList)
    {
        this.authList = authList;
    }
}
