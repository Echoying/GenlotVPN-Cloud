package com.ruoyi.yianlian.client.dto;

import com.ruoyi.yianlian.client.YiAnLianBase.YiAnLianRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 创建应用组请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("创建应用组请求")
public class YiAnLianServiceGroupCreateRequest extends YiAnLianRequest
{
    @ApiModelProperty(value = "应用组名称", required = true)
    private String name;

    @ApiModelProperty(value = "描述")
    private String description;

    @ApiModelProperty(value = "上级应用组id，默认为0", required = true)
    private String parentId;

    @ApiModelProperty(value = "上级应用分组路径", required = true)
    private String path;
}
