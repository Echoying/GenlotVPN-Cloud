package com.ruoyi.yianlian.client.dto;

import com.ruoyi.yianlian.client.YiAnLianBase.YiAnLianRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 修改应用组请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("修改应用组请求")
public class YiAnLianServiceGroupUpdateRequest extends YiAnLianRequest
{
    @ApiModelProperty(value = "应用组id", required = true)
    private String id;

    @ApiModelProperty(value = "应用组名称", required = true)
    private String name;

    @ApiModelProperty(value = "描述")
    private String description;
}
