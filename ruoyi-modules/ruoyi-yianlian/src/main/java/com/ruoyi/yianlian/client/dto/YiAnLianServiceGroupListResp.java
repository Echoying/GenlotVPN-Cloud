package com.ruoyi.yianlian.client.dto;

import com.ruoyi.yianlian.client.dto.vo.YiAnLianServiceGroupVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 查询应用组列表响应
 */
@Data
@ApiModel("查询应用组列表响应")
public class YiAnLianServiceGroupListResp implements Serializable
{
    @ApiModelProperty(value = "应用组树形数据")
    private YiAnLianServiceGroupVO data;
}
