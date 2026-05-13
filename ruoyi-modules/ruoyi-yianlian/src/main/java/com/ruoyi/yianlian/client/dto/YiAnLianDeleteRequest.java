package com.ruoyi.yianlian.client.dto;

import lombok.Data;

import java.util.List;

/**
 * 易安联删除请求
 */
@Data
public class YiAnLianDeleteRequest
{
    /**
     * 要删除的ID列表
     */
    private List<String> ids;

    public YiAnLianDeleteRequest()
    {
    }

    public YiAnLianDeleteRequest(List<String> ids)
    {
        this.ids = ids;
    }
}
